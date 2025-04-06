# Visa Payment Network Manager

A comprehensive Java Swing application for managing and analyzing credit card payment transactions within the Visa payment network.

## Project Overview

The Visa Payment Network Manager is a database management system designed to track, analyze, and visualize transaction data across the credit card payment ecosystem. The application provides a rich GUI interface for viewing transaction patterns, analyzing bank performance, monitoring card usage, and performing CRUD operations on all database entities.

## Features

- **Dashboard Overview**: View summary statistics, transaction trends, and key performance indicators
- **Card Usage Analysis**: Track card distributions, transaction volumes, and success rates by card type
- **Bank Analysis**: Monitor issuing and acquiring bank performance with detailed transaction history
- **Peak Sales Analytics**: Visualize transaction patterns by time of day and day of week with:
  - Interactive time heatmaps
  - Pie charts showing distribution by hour and day
  - Bar charts for comparative analysis
  - Detailed data tables with metrics
- **Custom SQL Query Interface**: Execute and visualize custom SQL queries
- **CRUD Operations**: Create, read, update, and delete records for all database entities
- **Real-time Data Refresh**: Auto-refresh capabilities to ensure current data

## System Requirements

- Java 17 or higher
- MySQL 8.0 or higher
- Maven 3.6 or higher
- At least 4GB RAM
- 100MB free disk space

## Database Setup

1. Install MySQL if not already installed
2. Create the database using the provided SQL script:

```bash
mysql -u root -p < src/res/db/create_database.sql
```

3. Load sample data (optional):

```bash
mysql -u root -p visa_final_spring < src/res/db/sample_data.sql
```

4. Set up stored procedures and triggers:

```bash
mysql -u root -p visa_final_spring < src/res/db/stored_procedures.sql
mysql -u root -p visa_final_spring < src/res/db/triggers.sql
mysql -u root -p visa_final_spring < src/res/db/events.sql
```

## Application Setup

1. Clone the repository:

```bash
git clone https://github.com/yourusername/visa-payment-network-manager.git
cd visa-payment-network-manager
```

2. Update database connection settings in `src/main/java/payment/database/DatabaseManager.java`:

```java
private static final String DB_URL = "jdbc:mysql://localhost:3306/visa_final_spring";
private static final String DB_USER = "your_username"; // Change to your MySQL username
private static final String DB_PASSWORD = "your_password"; // Change to your MySQL password
```

3. Build the application with Maven:

```bash
mvn clean package
```

## Running the Application

Run the application using the generated JAR file:

```bash
java -jar target/visa-payment-network-manager-1.0-SNAPSHOT.jar
```

Or use Maven:

```bash
mvn exec:java -Dexec.mainClass="payment.gui.VisaPaymentNetworkManager"
```

## Usage Guide

### Dashboard Overview
- Displays summary statistics for transactions, active cards, and success rates
- Shows charts for transaction volume over time and card type distribution

### Card Usage Panel
- View card distribution by type with interactive pie chart
- Track transaction volumes with time-series charts
- Monitor success rates across different card types
- Explore transaction details in tabular format

### Bank Analysis Panel
- Select banks to view detailed performance metrics
- Analyze transaction volume, average amount, and success rates
- View transaction history for selected banks
- Compare performance between different banks

### Peak Sales Analysis
- Use interactive visualizations to identify high-volume periods
- Filter by transaction count or value
- Change timeframe to view patterns for today, last 7 days, last 30 days, or all time
- Analyze hourly and weekday distribution with pie charts, bar charts, and heatmaps

### Custom Query Panel
- Execute custom SQL queries against the database
- View results in tabular format
- Export query results (where applicable)

### CRUD Operations
- Select tables from dropdown menu
- Create new records with form interface
- Update existing records
- Delete records with confirmation dialog
- View all table data with real-time refresh

## Architecture

The application follows a layered architecture:

1. **Presentation Layer**: Java Swing GUI components in `payment.gui` package
2. **Service Layer**: Business logic and data processing
3. **Data Access Layer**: Database connectivity through `DatabaseManager` and `QueryManager`
4. **Database**: MySQL backend with tables for cards, transactions, banks, etc.

## Server-Side Components

The application leverages MySQL server-side components:

- **Stored Procedures**: For complex transaction processing and analytics
- **Triggers**: For maintaining data integrity and audit logging
- **Events**: For scheduled maintenance tasks

## Troubleshooting

### Database Connection Issues
- Verify MySQL is running: `systemctl status mysql` or `service mysql status`
- Check connection parameters in `DatabaseManager.java`
- Ensure the database and tables exist: `mysql -u root -p -e "SHOW DATABASES;"`

### Display Issues
- If charts don't render properly, ensure JFreeChart libraries are included
- For Linux users, install the appropriate font packages: `sudo apt-get install fonts-dejavu`

### Performance Issues
- Increase JVM heap size: `java -Xmx1024m -jar target/visa-payment-network-manager-1.0-SNAPSHOT.jar`
- Optimize database queries by adding appropriate indexes

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b new-feature`
3. Commit changes: `git commit -am 'Add new feature'`
4. Push to branch: `git push origin new-feature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Acknowledgments

- JFreeChart library for data visualization
- MySQL Connector/J for database connectivity
- Java Swing for the GUI components
