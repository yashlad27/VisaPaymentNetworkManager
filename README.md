# Visa Payment Network Manager - Final Project Report

## 1. README: Installation and Setup Guide

### System Requirements
- **Java Development Kit (JDK)** 11 or higher
- **MySQL** 8.0 or higher
- **Maven** 3.6.3 or higher (for dependency management)

### Required Libraries
- **Java Swing**: Built-in GUI toolkit (included with JDK)
- **JDBC MySQL Connector**: Version 8.0.23 or higher
- **JFreeChart**: Version 1.5.3 for data visualization
- **Log4j**: Version 2.14.1 for logging

### Installation Steps

1. **Install JDK 11+**
  - Download from: https://www.oracle.com/java/technologies/javase-jdk11-downloads.html
  - Set JAVA_HOME environment variable to your JDK installation directory

2. **Install MySQL 8.0+**
  - Download from: https://dev.mysql.com/downloads/mysql/
  - Create a user with the following credentials:
    - Username: `root`
    - Password: `test123`
  - Or modify the `DatabaseManager.java` file with your preferred credentials

3. **Install Maven**
  - Download from: https://maven.apache.org/download.cgi
  - Add Maven to your PATH environment variable

4. **Database Setup**
  - Run the SQL scripts in the following order:
    1. `src/res/db/create_database.sql`
    2. `src/res/db/insert_table.sql`
    3. `src/res/db/tables/audit_tables.sql`
    4. `src/res/db/triggers/transaction_triggers.sql`
    5. `src/res/db/functions/analytics_functions.sql`
    6. `src/res/db/events/events.sql`
    7. `src/res/db/events/maintenance_events.sql`

5. **Project Setup**
  - Clone the repository or extract the project files
  - Navigate to the project root directory
  - Run `mvn clean install` to download dependencies and build the project

6. **Running the Application**
  - Execute `mvn exec:java -Dexec.mainClass="payment.gui.VisaPaymentNetworkManager"` to start the application
  - Or run the compiled JAR file with `java -jar visa-payment-network-manager.jar`

### Expected Directory Structure
```
VisaPaymentNetworkManager/
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   ├── payment/
│   │   │   │   ├── database/
│   │   │   │   │   ├── DatabaseManager.java
│   │   │   │   │   ├── QueryManager.java
│   │   │   │   │   └── ...
│   │   │   │   ├── gui/
│   │   │   │   │   ├── VisaPaymentNetworkManager.java
│   │   │   │   │   ├── DashboardPanel.java
│   │   │   │   │   ├── CardUsagePanel.java
│   │   │   │   │   ├── BankAnalysisPanel.java
│   │   │   │   │   ├── PeakSalesPanel.java
│   │   │   │   │   ├── QueryPanel.java
│   │   │   │   │   ├── CRUDPanel.java
│   │   │   │   │   └── ...
│   │   │   │   └── models/
│   │   │   └── ...
│   │   └── resources/
│   │       ├── db/
│   │       │   ├── create_database.sql
│   │       │   ├── insert_table.sql
│   │       │   ├── events/
│   │       │   ├── functions/
│   │       │   ├── queries/
│   │       │   ├── tables/
│   │       │   └── triggers/
│   └── test/
│
└── pom.xml
```

## 2. Technical Specifications

### Programming Languages
- **Java 11**: Core application language
- **SQL**: Database queries and schema definition

### Frameworks and Libraries
- **Java Swing**: GUI framework for creating desktop application
- **JDBC**: Java Database Connectivity for MySQL interaction
- **JFreeChart**: Library for creating charts and visualizations
- **MySQL**: Relational database management system

### Architecture
- **Three-tier architecture**:
  - Presentation Layer: Java Swing GUI components in `payment.gui` package
  - Business Logic Layer: Service classes handling data processing and business rules
  - Data Access Layer: Database connection and query execution in `payment.database` package

### Design Patterns
- **Singleton Pattern**: Used for `DatabaseManager` to ensure a single database connection instance
- **MVC Pattern**: Separation of data models, visual components, and business logic
- **Observer Pattern**: Used for real-time updates in dashboard components

### Key Components
- **Database Manager**: Handles connection pooling and query execution
- **Query Manager**: Manages predefined queries and query execution
- **GUI Panels**: Specialized panels for different aspects of the payment network
  - Dashboard Panel: Overview with key metrics
  - Card Usage Panel: Analysis of card type usage
  - Bank Analysis Panel: Performance metrics for banks
  - Peak Sales Panel: Analysis of transaction patterns
  - Query Panel: Custom SQL query execution
  - CRUD Panel: Database record management

## 3. Conceptual Design (UML Diagram)
![CS5200_Final_UML drawio](https://github.com/user-attachments/assets/f65ee6a5-9824-4f7d-8723-e2809f846e04)


## 4. Logical Database Design
![image](https://github.com/user-attachments/assets/d4bdc980-f6b9-47d2-8e90-a91b6dd3f6e8)


### Core Entity Tables
- **Cardholder**: Stores information about card holders
- **Card**: Stores credit card information with foreign key to Cardholder
- **IssuingBank**: Banks that issue cards to customers
- **AcquiringBank**: Banks that process card transactions for merchants
- **PaymentMerchant**: Merchants who accept card payments
- **Exchange**: Settlement exchanges between banks

### Transaction-Related Tables
- **Transaction**: Central table for all payment transactions
- **Authorization**: Authorization requests for transactions
- **AuthResponse**: Responses to authorization requests
- **Settlement**: Settlement records for approved transactions
- **InterchangeFee**: Fee structures for different card types

### Audit and Analytics Tables
- **CardStatusLog**: Tracks changes to card statuses
- **TransactionAuditLog**: Audit trail for transaction status changes
- **ArchivedTransactions**: Storage for old transactions
- **BankPerformanceReport**: Aggregated bank performance metrics
- **DailyTransactionSummary**: Daily transaction statistics

### Database Features
- **Normalization**: Tables are normalized to 3NF
- **Constraints**: Primary keys, foreign keys, unique constraints
- **Triggers**: Automated data validation and audit logging
- **Stored Procedures**: Predefined database operations
- **Functions**: Reusable calculations for analytics
- **Events**: Scheduled tasks for maintenance and reporting

### Key Relationships
- A Cardholder can have multiple Cards
- A Card belongs to one IssuingBank
- A Transaction involves one Card, one PaymentMerchant, and one AcquiringBank
- A Transaction has one Authorization, which can have one AuthResponse
- Successful Transactions have one Settlement record
- Each Exchange has multiple InterchangeFee records for different card types

## 5. User Flow and Interaction

### Application Launch
1. User starts the Visa Payment Network Manager application
2. The application connects to the MySQL database
3. The main window appears with the Dashboard Overview tab active

### Dashboard Overview Tab
1. User views key performance indicators (KPIs)
  - Total transactions
  - Total value
  - Today's metrics compared to yesterday
  - Success rate
2. User examines card type distribution chart
3. User reviews transaction status breakdown
4. User analyzes hourly transaction volume patterns
5. User views top merchants table

### Card Usage Tab
1. User views card distribution statistics
  - Total cards
  - Active cards
  - Transaction counts and volumes
2. User reviews card type pie chart and transaction volume charts
3. User selects a card type from the table
4. User views detailed transaction history for the selected card type

### Bank Analysis Tab
1. User views bank performance metrics
2. User selects a bank from the table
3. User reviews detailed bank information
  - Success rate
  - Total transactions
  - Average transaction amount
  - Unique merchants
4. User examines transaction history for the selected bank

### Peak Sales Tab
1. User selects view mode (Transaction Count or Transaction Value)
2. User selects timeframe (Today, Last 7 Days, Last 30 Days, All Time)
3. User examines hourly transaction distribution
4. User switches to the Weekday Analysis tab
5. User examines transaction patterns by day of week
6. User switches to the Time Heatmap tab
7. User views time-based heatmap showing transaction density

### Custom Query Tab
1. User selects a saved query from the dropdown or writes a custom SQL query
2. User clicks "Execute" to run the query
3. User views the query results in the table
4. User can save the query for future use

### CRUD Operations Tab
1. User selects a database table from the dropdown
2. User views existing records in the table
3. User performs database operations:
  - Create: Fill in the form and click "Create" to add a new record
  - Read: Select a record to view its details
  - Update: Modify fields of a selected record and click "Update"
  - Delete: Select a record and click "Delete" to remove it

### Data Refresh
- Automatic refresh: All panels refresh data at regular intervals
- Manual refresh: User can click "Refresh" buttons to update data immediately

## 6. Lessons Learned

### Technical Expertise Gained

#### Database Design and Optimization
- Designed a complex relational database schema with multiple interconnected tables
- Implemented normalization principles to reduce data redundancy
- Created database constraints to maintain data integrity
- Developed triggers, stored procedures, and events for automated database operations
- Gained experience with transaction processing and audit logging

#### Java Swing GUI Development
- Built a responsive desktop application using Java Swing
- Implemented custom components for data visualization
- Created reusable UI patterns for consistent user experience
- Managed background threads for responsive UI during data operations
- Developed real-time data updates with automatic refresh mechanisms

#### Data Visualization Techniques
- Implemented various chart types (pie, bar, line) using JFreeChart
- Created a custom heatmap visualization for time-based data analysis
- Designed interactive dashboards with drill-down capabilities
- Formatted numerical data for better readability and comprehension

#### Database Connectivity
- Implemented a robust database connection manager with proper resource handling
- Created parameterized queries to prevent SQL injection
- Designed a query caching system for improved performance
- Developed error handling and user feedback for database operations

### Insights and Challenges

#### Time Management
- Complex UI components required more time than initially estimated
- Database schema design iterations took longer than expected
- Breaking the project into smaller, manageable modules helped maintain progress
- Setting intermediate milestones provided better visibility into project status

#### Data Domain Insights
- Credit card transaction processing involves multiple parties and steps
- Real-time analytics require efficient data processing and presentation
- Financial data visualization needs careful consideration for accuracy and clarity
- Authentication and authorization workflows are complex in payment processing

#### Technical Challenges
- Managing concurrent database access required careful design
- Large result sets needed efficient processing to avoid memory issues
- Real-time updates required balancing refresh frequency with performance
- Handling different data types and formats across the database schema

### Alternative Design Approaches

#### Architecture Alternatives
- **Web Application**: Considered implementing as a web application with Spring Boot and React
  - Pros: Easier deployment, cross-platform accessibility
  - Cons: More complex setup, additional security considerations
  - Decision: Chose desktop application for simplicity and direct database access

#### Database Design Alternatives
- **NoSQL Approach**: Considered MongoDB for flexible schema
  - Pros: Easier to evolve schema, potentially better performance for certain queries
  - Cons: Less structured relationships, less support for complex transactions
  - Decision: Chose relational database for strong transaction support and integrity

#### UI Design Alternatives
- **JavaFX vs. Swing**: Considered JavaFX for modern UI components
  - Pros: More modern look, better styling options
  - Cons: Steeper learning curve, additional dependencies
  - Decision: Chose Swing for compatibility and simplicity

### Issues and Limitations

#### Known Issues
- The CRUDPanel doesn't handle all data types correctly (e.g., BLOB, JSON)
- Some charts may not scale correctly with very large datasets
- The database connection doesn't implement proper connection pooling
- Query timeout handling is not fully implemented

## 7. Future Work

### Planned Database Enhancements
1. **Data Partitioning**: Implement table partitioning for transaction history
2. **Performance Optimization**: Add additional indexes for common query patterns
3. **Advanced Analytics**: Create more complex stored procedures for trend analysis
4. **Data Archiving**: Implement a comprehensive archiving strategy for historical data

### Potential Feature Additions
1. **User Authentication**: Add user accounts with role-based access control
2. **Transaction Simulation**: Create a module to simulate and test transaction flows
3. **Fraud Detection**: Implement machine learning models for fraud pattern detection
4. **Report Generation**: Add export functionality for reports in PDF/Excel formats
5. **Mobile Companion App**: Develop a mobile application for on-the-go monitoring

### Integration Opportunities
1. **API Integration**: Create REST APIs to integrate with external systems
2. **Real-time Alerts**: Implement notification system for anomalous transactions
3. **Data Import/Export**: Add support for importing external data sources
4. **Compliance Reporting**: Add modules for regulatory reporting requirements

### Performance Improvements
1. **Connection Pooling**: Implement proper database connection pooling
2. **Query Optimization**: Analyze and optimize slow-running queries
3. **Caching Strategy**: Implement a more sophisticated caching system
4. **UI Responsiveness**: Improve background thread management for smoother UI
