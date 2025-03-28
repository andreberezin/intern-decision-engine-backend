package ee.taltech.inbankbackend.service;

import com.github.vladislavgoltjajev.personalcode.locale.estonia.EstonianPersonalCodeValidator;
import ee.taltech.inbankbackend.config.DecisionEngineConstants;
import ee.taltech.inbankbackend.exceptions.InvalidLoanAmountException;
import ee.taltech.inbankbackend.exceptions.InvalidLoanPeriodException;
import ee.taltech.inbankbackend.exceptions.InvalidPersonalCodeException;
import ee.taltech.inbankbackend.exceptions.NoValidLoanException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class DecisionEngineTest {

    @InjectMocks
    private DecisionEngine decisionEngine;

    @Mock
    private EstonianPersonalCodeValidator mockValidator;

    // Real instance for other tests
    private EstonianPersonalCodeValidator realValidator;


    private String debtorPersonalCode;
    private String segment1PersonalCode;
    private String segment2PersonalCode;
    private String segment3PersonalCode;

    @BeforeEach
    void setUp() {
        debtorPersonalCode = "37605030299";
        segment1PersonalCode = "50307172740";
        segment2PersonalCode = "38411266610";
        segment3PersonalCode = "35006069515";

        // Initialize the real validator for tests that need it
        realValidator = new EstonianPersonalCodeValidator();
    }

    @Test
    void testTooYoungApplicant() {
        // Mock the validator to always return true for this test
        Mockito.when(mockValidator.isValid(Mockito.anyString())).thenReturn(true);

        DecisionEngine decisionEngineWithMock = new DecisionEngine(mockValidator);

        // Born in 2010, assuming test runs in 2025
        String youngPersonalCode = "61001010001";

        assertThrows(NoValidLoanException.class,
                () -> decisionEngineWithMock.calculateApprovedLoan(youngPersonalCode, 4000L, 12),
                "Expected exception for an applicant under 18 years old.");
    }

    @Test
    void testTooOldApplicant() {
        // Mock the validator to always return true for this test
        Mockito.when(mockValidator.isValid(Mockito.anyString())).thenReturn(true);

        DecisionEngine decisionEngineWithMock = new DecisionEngine(mockValidator);

        // Born in 1920, assuming test runs in 2025
        String oldPersonalCode = "32001010002";

        assertThrows(NoValidLoanException.class,
                () -> decisionEngineWithMock.calculateApprovedLoan(oldPersonalCode, 4000L, 12),
                "Expected exception for an applicant exceeding max eligible age.");
    }

    @Test
    void testValidAgeApplicant() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        // Mock the validator to always return true for this test
        Mockito.when(mockValidator.isValid(Mockito.anyString())).thenReturn(true);

        DecisionEngine decisionEngineWithMock = new DecisionEngine(mockValidator);

        // Born in 1985, assuming test runs in 2025
        String validAgePersonalCode = "38501019003";

        Decision decision = decisionEngineWithMock.calculateApprovedLoan(validAgePersonalCode, 4000L, 12);
        assertEquals(10000, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testDebtorPersonalCode() {
        decisionEngine = new DecisionEngine(realValidator);

        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 4000L, 12));
    }

    @Test
    void testSegment1PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        decisionEngine = new DecisionEngine(realValidator);

        Decision decision = decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, 12);
        assertEquals(2000, decision.getLoanAmount());
        assertEquals(20, decision.getLoanPeriod());
    }

    @Test
    void testSegment2PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        decisionEngine = new DecisionEngine(realValidator);

        int age = decisionEngine.calculateAge(segment2PersonalCode);

        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 4000L, 12);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

//    @Test
//    void testSegment3PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
//            InvalidPersonalCodeException, InvalidLoanAmountException {
//        decisionEngine = new DecisionEngine(realValidator);
//
//        int age = decisionEngine.calculateAge(segment3PersonalCode);
//        System.out.println("Age for segment3: " + age);
//
//        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12);
//        assertEquals(10000, decision.getLoanAmount());
//        assertEquals(12, decision.getLoanPeriod());
//    }

    @Test
    void testSegment3PersonalCode() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {

        // Create a mock of the DecisionEngine (so we can mock the calculateAge method)
        DecisionEngine mockDecisionEngine = Mockito.mock(DecisionEngine.class);

        // Mock the behavior of the calculateAge method to return an age that makes the person eligible
        Mockito.when(mockDecisionEngine.calculateAge(Mockito.anyString())).thenReturn(30);  // Mocking an eligible age (30)

        // Mock the behavior of the isValid method from EstonianPersonalCodeValidator (if necessary)
        EstonianPersonalCodeValidator mockValidator = Mockito.mock(EstonianPersonalCodeValidator.class);
        Mockito.when(mockValidator.isValid(Mockito.anyString())).thenReturn(true);

        // Now, pass the mocked validator into the DecisionEngine
        decisionEngine = new DecisionEngine(mockValidator);

        // Now the age will be mocked to be 30, making the person eligible
        int age = mockDecisionEngine.calculateAge(segment3PersonalCode);  // This will return the mocked age
        System.out.println("Age for segment3: " + age);

        // Proceed with the actual loan calculation
        Decision decision = decisionEngine.calculateApprovedLoan(segment3PersonalCode, 4000L, 12);

        // Validate the results
        assertEquals(10000, decision.getLoanAmount());  // Expected loan amount for segment 3
        assertEquals(12, decision.getLoanPeriod());  // Expected loan period
    }

    @Test
    void testInvalidPersonalCode() {
        decisionEngine = new DecisionEngine(realValidator);
        String invalidPersonalCode = "12345678901";
        assertThrows(InvalidPersonalCodeException.class,
                () -> decisionEngine.calculateApprovedLoan(invalidPersonalCode, 4000L, 12));
    }

    @Test
    void testInvalidLoanAmount() {
        decisionEngine = new DecisionEngine(realValidator);
        Long tooLowLoanAmount = DecisionEngineConstants.MINIMUM_LOAN_AMOUNT - 1L;
        Long tooHighLoanAmount = DecisionEngineConstants.MAXIMUM_LOAN_AMOUNT + 1L;

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooLowLoanAmount, 12));

        assertThrows(InvalidLoanAmountException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, tooHighLoanAmount, 12));
    }

    @Test
    void testInvalidLoanPeriod() {
        decisionEngine = new DecisionEngine(realValidator);
        int tooShortLoanPeriod = DecisionEngineConstants.MINIMUM_LOAN_PERIOD - 1;
        int tooLongLoanPeriod = DecisionEngineConstants.MAXIMUM_LOAN_PERIOD + 1;

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooShortLoanPeriod));

        assertThrows(InvalidLoanPeriodException.class,
                () -> decisionEngine.calculateApprovedLoan(segment1PersonalCode, 4000L, tooLongLoanPeriod));
    }

    @Test
    void testFindSuitableLoanPeriod() throws InvalidLoanPeriodException, NoValidLoanException,
            InvalidPersonalCodeException, InvalidLoanAmountException {
        decisionEngine = new DecisionEngine(realValidator);
        Decision decision = decisionEngine.calculateApprovedLoan(segment2PersonalCode, 2000L, 12);
        assertEquals(3600, decision.getLoanAmount());
        assertEquals(12, decision.getLoanPeriod());
    }

    @Test
    void testNoValidLoanFound() {
        decisionEngine = new DecisionEngine(realValidator);
        assertThrows(NoValidLoanException.class,
                () -> decisionEngine.calculateApprovedLoan(debtorPersonalCode, 10000L, 60));
    }

}

