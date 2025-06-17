# Selenium Java Automation Framework

## Overview
This project is a Selenium-based automation framework in Java 11, following the Page Object Model (POM) pattern. It uses TestNG for test management and WebDriverManager for automatic driver management. The framework is designed for maintainability, scalability, and ease of use.

## Features
- **Java 11**
- **Selenium WebDriver** for browser automation
- **TestNG** for test execution and reporting
- **WebDriverManager** for automatic driver binaries
- **Page Object Model (POM)** for clean separation of page logic
- **Reusable utilities** for table sorting validation
- **Maven** for build and dependency management

## Project Structure
```
selenium-framework/
├── pom.xml                  # Maven configuration
├── README.md                # Project documentation
├── src/
│   ├── main/java/com/example/selenium/
│   │   ├── core/            # BasePage, DriverManager
│   │   ├── pages/           # MainPage, SortableDataTablesPage
│   │   └── utils/           # TableUtils
│   └── test/java/com/example/selenium/
│       └── tests/           # Test classes (e.g., SortableDataTablesTest)
└── ...
```

## How to Run

### Prerequisites
- Java 11+
- Maven 3.6+
- Chrome browser (for default WebDriver)

### Steps
1. **Clone the repository** (if not already done):
   ```bash
   git clone <your-repo-url>
   cd selenium-framework
   ```
2. **Install dependencies and build the project:**
   ```bash
   mvn clean compile
   ```
3. **Run the tests:**
   ```bash
   mvn test
   ```
   This will launch Chrome, navigate to the test site, and execute the automated tests. Results will be shown in the console and in the `target/surefire-reports` directory.

## Adding Tests
- Add new test classes in `src/test/java/com/example/selenium/tests/`.
- Create new Page Objects in `src/main/java/com/example/selenium/pages/`.
- Use utility methods from `src/main/java/com/example/selenium/utils/` as needed.

## Customization
- Update `DriverManager` to use a different browser if needed.
- Extend `BasePage` for new page objects.
- Add more utility methods for common actions and assertions.

## Troubleshooting
- If you see browser or driver errors, ensure your Chrome browser is up to date.
- For SLF4J warnings, you can ignore them or add a logger dependency if desired.
- If tests fail, check the console output and `target/surefire-reports` for details.

## Credits
- [Selenium WebDriver](https://www.selenium.dev/)
- [TestNG](https://testng.org/)
- [WebDriverManager](https://github.com/bonigarcia/webdrivermanager)

---
Feel free to extend this framework for your own automation needs!
