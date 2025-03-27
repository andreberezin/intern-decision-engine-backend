package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;

/**
 * A service class that provides a method for calculating an approved loan amount and period for a customer.
 * The loan amount is calculated based on the customer's credit modifier,
 * which is determined by the last four digits of their ID code.
 */
@Service
public class DecisionEngine {

    // Used to check for the validity of the presented ID code.
    private final EstonianPersonalCodeValidator validator;
    private int creditModifier = 0;

    // default constructor for production
    public DecisionEngine() {
        this.validator = new EstonianPersonalCodeValidator();
    }

    // constructor for testing age validity
    public DecisionEngine(EstonianPersonalCodeValidator validator) {
        this.validator = validator;
    }


    /**
     * Calculates the maximum loan amount and period for the customer based on their ID code,
     * the requested loan amount and the loan period.
     * The loan period must be between 12 and 60 months (inclusive).
     * The loan amount must be between 2000 and 10000€ months (inclusive).
     *
     * @param personalCode ID code of the customer that made the request.
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @return A Decision object containing the approved loan amount and period, and an error message (if any)
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     * @throws NoValidLoanException If there is no valid loan found for the given ID code, loan amount and loan period
     */
    public Decision calculateApprovedLoan(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException,
            NoValidLoanException {
        try {
            verifyInputs(personalCode, loanAmount, loanPeriod);
        } catch (Exception e) {
            return new Decision(null, null, e.getMessage());
        }

        creditModifier = getCreditModifier(personalCode);

        if (creditModifier == 0) {
            throw new NoValidLoanException("No valid loan found!");
        }

        int maxApprovedLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT;

        // determine the maximum approved loan amount
        for (int amount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT;
            amount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT;
            amount += 100) {

            if (isLoanApproved(amount, loanPeriod)) {
                maxApprovedLoanAmount = amount;
            }
        }

        // if loan is not approved then find out if it can be approved for a longer loan period
        while (!isLoanApproved(maxApprovedLoanAmount, loanPeriod) &&
                loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            loanPeriod++;
        }


        if (loanPeriod > DecisionEngineConstants.MAXIMUM_LOAN_PERIOD) {
            throw new NoValidLoanException("No valid loan found within allowed loan periods.");
        }


        return new Decision(maxApprovedLoanAmount, loanPeriod, null);
    }


    // calculate user's age from their personal code
    public int calculateAge(String personalCode) {
        LocalDate currentDate = LocalDate.now();
        String year = personalCode.substring(1, 3);
        String month = personalCode.substring(3, 5);
        String day = personalCode.substring(5, 7);
        String centuryIndicator = personalCode.substring(0, 1);
        String century = switch (centuryIndicator) {
            case "1", "2" -> "18";
            case "3", "4" -> "19";
            case "5", "6" -> "20";
            case "7", "8" -> "21";
            default -> throw new IllegalArgumentException("Invalid century indicator in personal code");
        };

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate birthDate = LocalDate.parse(century + year + "-" + month + "-" + day, formatter);

        int age = Period.between(birthDate, currentDate).getYears();

        return age;
    }

    // check if user is eligible by age
    public boolean isEligibleByAge(int age) {
        int maxAge = DecisionEngineConstants.LIFE_EXPECTANCY_ESTONIA - (DecisionEngineConstants.MAXIMUM_LOAN_PERIOD / 12);
        System.out.println("Max Age: " + maxAge + ", Age: " + age);
        if (age < 18 || age > maxAge) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Calculates the credit score for the current loan amount and loan period to determine if loan is approved or not.
     *
     * @return boolean
     */
    private boolean isLoanApproved(long loanAmount, int loanPeriod) {

        double creditScore = ((double) creditModifier / loanAmount) * loanPeriod / 10;

        return creditScore >= 0.1;
    }

    /**
     * Calculates the credit modifier of the customer to according to the last four digits of their ID code.
     * Debt - 0000...2499
     * Segment 1 - 2500...4999
     * Segment 2 - 5000...7499
     * Segment 3 - 7500...9999
     *
     * @param personalCode ID code of the customer that made the request.
     * @return Segment to which the customer belongs.
     */
    private int getCreditModifier(String personalCode) {
        int segment = Integer.parseInt(personalCode.substring(personalCode.length() - 4));

        if (segment < 2500) {
            return 0;
        } else if (segment < 5000) {
            return DecisionEngineConstants.SEGMENT_1_CREDIT_MODIFIER;
        } else if (segment < 7500) {
            return DecisionEngineConstants.SEGMENT_2_CREDIT_MODIFIER;
        }

        return DecisionEngineConstants.SEGMENT_3_CREDIT_MODIFIER;
    }

    /**
     * Verify that all inputs are valid according to business rules.
     * If inputs are invalid, then throws corresponding exceptions.
     *
     * @param personalCode Provided personal ID code
     * @param loanAmount Requested loan amount
     * @param loanPeriod Requested loan period
     * @throws InvalidPersonalCodeException If the provided personal ID code is invalid
     * @throws InvalidLoanAmountException If the requested loan amount is invalid
     * @throws InvalidLoanPeriodException If the requested loan period is invalid
     */
    private void verifyInputs(String personalCode, Long loanAmount, int loanPeriod)
            throws InvalidPersonalCodeException, InvalidLoanAmountException, InvalidLoanPeriodException, NoValidLoanException {

        if (!validator.isValid(personalCode)) {
            throw new InvalidPersonalCodeException("Invalid personal ID code!");
        }

        int age = calculateAge(personalCode);

        if (!isEligibleByAge(age)) {
            throw new NoValidLoanException("Not eligible due to age.");
        }

        if (!(DecisionEngineConstants.MINIMUM_LOAN_AMOUNT <= loanAmount)
                || !(loanAmount <= DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT)) {
            throw new InvalidLoanAmountException("Invalid loan amount!");
        }
        if (!(DecisionEngineConstants.MINIMUM_LOAN_PERIOD <= loanPeriod)
                || !(loanPeriod <= DecisionEngineConstants.MAXIMUM_LOAN_PERIOD)) {
            throw new InvalidLoanPeriodException("Invalid loan period!");
        }

    }
}
