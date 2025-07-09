# Task Report Generator

A Node.js application for generating daily, weekly, and custom date range reports from task data.

## Features

- Fetch task data from an API endpoint
- Generate reports for a specific day, week, or custom date range
- Email functionality with customizable fields (Subject, To, CC)
- Display task data in sortable, filterable tables
- Preview reports before sending them
- Generate formatted reports grouped by team

## Installation

1. Clone the repository:

```bash
git clone <repository-url>
cd task-report-nodejs
```

2. Install dependencies:

```bash
npm install
```

3. Create a `.env` file with your configuration:

```
PORT=3000
API_BASE_URL=http://your-api-url
DEFAULT_BOARD_ID=1
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USER=your-email@example.com
SMTP_PASS=your-password
```

## Usage

### Development Mode

To run the application in development mode with auto-reload:

```bash
npm run dev
```

### Production Mode

To run the application in production mode:

```bash
npm start
```

The application will be accessible at `http://localhost:3000` (or the port specified in your `.env` file).

## Project Structure

```
task-report-nodejs/
├── src/
│   ├── app.js                  # Main application file
│   ├── controllers/            # Request handlers
│   │   ├── taskController.js
│   │   └── reportController.js
│   ├── models/                 # Data models
│   │   └── taskModel.js
│   ├── public/                 # Static assets
│   │   ├── css/
│   │   │   └── style.css
│   │   └── js/
│   │       └── main.js
│   ├── routes/                 # Express routes
│   │   ├── taskRoutes.js
│   │   └── reportRoutes.js
│   └── views/                  # EJS templates
│       ├── index.ejs
│       ├── reports.ejs
│       ├── error.ejs
│       ├── 404.ejs
│       ├── reports/
│       │   ├── daily.ejs
│       │   ├── weekly.ejs
│       │   ├── custom.ejs
│       │   ├── preview.ejs
│       │   └── success.ejs
│       └── partials/
│           ├── header.ejs
│           ├── footer.ejs
│           └── task-table.ejs
├── .env                        # Environment variables
├── package.json
└── README.md
```

## API Endpoints

- `/api/tasks` - Get all tasks
- `/api/tasks/by-date` - Get tasks by date
- `/api/tasks/by-range` - Get tasks by date range

## License

MIT
