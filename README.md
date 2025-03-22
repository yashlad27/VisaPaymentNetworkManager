# Visa Payment Network Manager

A Java Swing desktop application for managing and analyzing credit card payment transaction data across the Visa payment network. This application provides real-time visualization and analytics for transaction patterns, bank performance, and card usage.

## Project Overview

The Visa Payment Network Manager is a comprehensive solution for payment processors, financial institutions, and network administrators who need to monitor, analyze, and manage payment transaction data. The application connects to a MySQL database to provide real-time visualization of transaction flows, payment patterns, and performance metrics.

Key features include:
- Interactive dashboards with transaction metrics
- Card usage analysis with visualization
- Bank performance monitoring
- Peak sales analysis with time-based heatmaps
- Custom SQL query interface
- CRUD operations for database management

## Technology Stack

- **Backend:** Java 8+
- **Frontend:** Java Swing
- **Database:** MySQL
- **Build Tool:** Maven

## Screenshots

<img width="1312" alt="Screenshot 2025-03-21 at 8 07 37 PM" src="https://github.com/user-attachments/assets/eb8b747a-462e-45c7-b29c-aa70b07b1ea1" />
<img width="1312" alt="Screenshot 2025-03-21 at 8 07 49 PM" src="https://github.com/user-attachments/assets/17e73f6b-4d73-47e5-bb5b-2e7a24e0d63e" />
<img width="1312" alt="Screenshot 2025-03-21 at 8 08 04 PM" src="https://github.com/user-attachments/assets/aa1a351a-8dad-4029-a06c-5d86bbcaa0d4" />
<img width="1312" alt="Screenshot 2025-03-21 at 8 08 12 PM" src="https://github.com/user-attachments/assets/7aa5d6a4-a37b-441e-ac03-1ad154ea20cc" />
<img width="1312" alt="Screenshot 2025-03-21 at 8 08 25 PM" src="https://github.com/user-attachments/assets/74624b7a-9085-42c1-89bf-502b03dcfbe4" />
<img width="1312" alt="Screenshot 2025-03-21 at 8 08 42 PM" src="https://github.com/user-attachments/assets/d7decc53-b45b-4856-bfac-463a6b588f14" />



## Prerequisites

- Java 8 or higher
- MySQL 5.7 or higher
- Maven 3.6 or higher (for building from source)

## Database Setup

1. Install MySQL on your system if not already installed
2. Create a new database:
   ```sql
   CREATE DATABASE visa_final_spring;
   ```
3. Run the database setup scripts:
   ```
   mysql -u username -p visa_final_spring < src/res/db/create_database.sql
   mysql -u username -p visa_final_spring < src/res/db/insert_table.sql
   ```

## Build & Run

### From Source

1. Clone the repository:
   ```
   git clone https://github.com/yourusername/VisaPaymentNetworkManager.git
   cd VisaPaymentNetworkManager
   ```

2. Build the project using Maven:
   ```
   mvn clean package
   ```

3. Run the application:
   ```
   java -jar target/visa-payment-network-manager.jar
   ```

### Using Pre-built JAR

1. Download the latest release JAR file
2. Run the application:
   ```
   java -jar visa-payment-network-manager.jar
   ```

## Configuration

Database connection parameters can be modified in the `DatabaseManager.java` file:
```java
private static final String DEFAULT_URL = "jdbc:mysql://localhost:3306/visa_final_spring";
private static final String DEFAULT_USER = "root";
private static final String DEFAULT_PASSWORD = "test123";
```

## Data Generation

A Python script is included to generate synthetic transaction data for testing purposes:

1. Install Python 3.6+ if not already installed
2. Run the data generator:
   ```
   python data_generator.py --transactions 500 --output generated_data.sql --start-date 2025-01-01 --end-date 2025-05-31
   ```
3. Import the generated data:
   ```
   mysql -u username -p visa_final_spring < generated_data.sql
   ```

## Features

### Dashboard Overview
- Transaction count and volume metrics
- Success rate monitoring
- Card type distribution
- Top merchants tracking
- Transaction status breakdown visualization
- Hourly transaction volume chart

### Card Usage Analysis
- Card type distribution analysis
- Popular cards by transaction volume
- Trend analysis for card types

### Bank Analysis
- Issuing bank performance metrics
- Acquiring bank metrics
- Approval rate analysis
- Processing time tracking

### Peak Sales Analysis
- Time-based heatmap visualization
- Hourly transaction distribution
- Weekday transaction patterns

### Custom SQL Query Interface
- Execute custom SQL queries
- Save and load common queries
- Export query results

### CRUD Operations
- Create, read, update, and delete records
- Dynamic form generation for any table
- Validation and error handling

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- Northeastern University CS5200 Database Management course
- Java Swing and MySQL communities
