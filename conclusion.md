# Ticket-101: Backend Code Review

### What Went Well:
1. Code Structure:
- The intern mostly followed java coding standards, for example how he structured the project into layers of service, endpoints and so on. 
- Extending base classes instead of modifying them (e.g., class InvalidLoanPeriodException extends Throwable) and having separate methods and classes (e.g. Decision, DecisionResponse and DecisionRequest) for each purpose is a good practice to make code maintainable and reduce complexity.
- Using Lombok is a great choice because it helps avoid unnecessary work which also helps avoid simple mistakes and reduces the amount of code.
- The intern included comments in the necessary parts of the code which can help avoid having to remember what each part does.

2. Ensuring everything works:
- The intern took the initiative to add tests for different personal codes which would have different credit modifiers to ensure each possibility is covered. This is a good practice to ensure functionality and avoid bugs.
-  Furthermore, having custom exceptions for different situations shows that they thought ahead and are prepared for them.

⸻

### Areas for Improvement:
- While the intern had the correct idea about structuring the project, entities and DTO-s such DecisionRequest, DecisionResponse and Decision could be separated from the controller into a separate directory.
- Potentially some parts of the code could be split into smaller parts to make it more readable and maintainable. For example the verifyInputs method in DecisionEngine is responsible for checking 3 inputs. This could be split into 3 different methods which check 1 input each.
- Dependency injection and abstract classes could be used so higher level components don't directly depend on concrete classes. For example EstonianPersonalCodeValidator could be injected into DecisionEngine rather than directly creating and instance of it.

⸻

### Important shortcoming
- The most important shortcoming was having an incorrect scoring algorithm in the calculateApprovedLoan method in DecisionEngine.java. The instructions state "credit score = ((credit modifier / loan amount) * loan period) / 10" which was not included in the code. \
This issue has now been fixed.

⸻

### Conclusion:

Overall the intern did great work writing maintainable code and making sure to avoid unnecessary bugs and work later on. Some improvements in the code structure could be made and double-checking the code by reading the instructions again once the code is finished is a good practice as well to make sure that the code written matches the instructions.

⸻

