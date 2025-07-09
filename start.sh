#!/bin/bash
# filepath: /opt/lampp/htdocs/sangle_workspace/report_project/task-report-nodejs/start.sh

# Define colors for output
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}Task Report Generator${NC}"
echo -e "${YELLOW}Starting application...${NC}"

# Check if node is installed
if ! command -v node &> /dev/null; then
    echo "Node.js is not installed. Please install Node.js to run this application."
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo "npm is not installed. Please install npm to run this application."
    exit 1
fi

# Install dependencies if node_modules doesn't exist
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing dependencies...${NC}"
    npm install
fi

# Check if .env file exists
if [ ! -f ".env" ]; then
    echo -e "${YELLOW}Creating .env file...${NC}"
    echo "PORT=3000" > .env
    echo "API_BASE_URL=http://172.18.100.184/mini_project/public/api" >> .env
    echo "DEFAULT_BOARD_ID=1" >> .env
    echo "SMTP_HOST=smtp.example.com" >> .env
    echo "SMTP_PORT=587" >> .env
    echo "SMTP_USER=your-email@example.com" >> .env
    echo "SMTP_PASS=your-password" >> .env
    echo -e "${GREEN}.env file created with default values${NC}"
fi

# Check if running in development or production
if [ "$1" == "dev" ]; then
    echo -e "${GREEN}Starting in development mode...${NC}"
    npm run dev
else
    echo -e "${GREEN}Starting in production mode...${NC}"
    npm start
fi
