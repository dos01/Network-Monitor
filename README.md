# Network Intelligence Monitor

A professional, real-time network traffic monitoring application built with **JavaFX**. This tool provides a sleek dashboard to visualize incoming and outgoing data, track historical usage, and analyze network trends over various time periods.

## Features

- **Real-Time Monitoring**: Live visualization of download and upload speeds.
- **Dynamic Time Filters**: Analyze data across multiple time windows (5 Min to 1 Month).
- **Cumulative Usage Tracking**: Displays total received and sent data for any selected period.
- **Usage Quotas & Alerts**: Set monthly data limits with a real-time progress tracker on the dashboard and threshold alerts.
- **Data Export**: Export aggregated daily network usage records to **CSV** format.
- **Database Maintenance**: 
  - **Auto-Cleanup**: Automatically deletes data older than 1 year to maintain performance.
  - **Manual Purge**: Clear history for current filters or all-time via Settings.
- **Persistent Storage**: All network statistics are stored locally using **SQLite**.
- **Premium UI**: Modern, dark-themed interface with responsive micro-animations and smooth charts.

## Tech Stack

- **Language**: Java 17+
- **UI Framework**: JavaFX
- **Graphics**: FXML & Vanilla CSS
- **Database**: SQLite (via JDBC)
- **Build Tool**: Maven

## Development

Work is ongoing on the `development` branch. All pull requests should be targeted there.

### Prerequisites

- Java Development Kit (JDK) 17 or higher
- Apache Maven

### Installation & Running

1. **Clone the repository**:
   ```bash
   git clone https://github.com/dos01/Network-Monitor.git
   cd Network-Monitor
   ```

2. **Build and Run the application**:
   ```bash
   mvn clean javafx:run
   ```

## Project Structure

- `src/main/java/com/networkmonitor/ui/`: Contains the JavaFX controllers and UI logic.
- `src/main/java/com/networkmonitor/service/`: Core logic for network tracking and database management.
- `src/main/resources/`: FXML layouts and CSS stylesheets.

---
Developed by **Dr. Doolittle**
