# Network Intelligence Monitor

A professional, real-time network traffic monitoring application built with **JavaFX**. This tool provides a sleek dashboard to visualize incoming and outgoing data, track historical usage, and analyze network trends over various time periods.

## Features

- **Real-Time Monitoring**: Live visualization of download and upload speeds.
- **Dynamic Time Filters**: Analyze data across multiple time windows:
  - 5 Min, 15 Min, 30 Min
  - 1 Hour (Live Mode)
  - 3 Hours, 24 Hours
  - 1 Week, 1 Month
- **Cumulative Usage Tracking**: Displays total received and sent data for any selected period.
- **Custom Range Selection**: Pick specific start and end dates to analyze historical data.
- **Persistent Storage**: All network statistics are stored locally using **SQLite** for long-term analysis.
- **Premium UI**: Modern, dark-themed interface with responsive micro-animations and smooth charts.

## Tech Stack

- **Lanuage**: Java 17+
- **UI Framework**: JavaFX
- **Graphics**: FXML & Vanilla CSS
- **Database**: SQLite (via JDBC)
- **Build Tool**: Maven

## Getting Started

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
