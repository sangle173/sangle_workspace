#!/bin/bash
# Script to push to a remote repository

# Set your GitHub username
GITHUB_USERNAME="your-username"

# Set your repository name
REPO_NAME="task-report-nodejs"

# Create a new repository on GitHub (you'll need to do this manually through the GitHub website)
echo "Please create a new repository on GitHub named '$REPO_NAME' at https://github.com/new"
echo "Once created, run the following commands:"
echo ""
echo "git remote add origin https://github.com/$GITHUB_USERNAME/$REPO_NAME.git"
echo "git branch -M main"
echo "git push -u origin main"
echo ""
echo "Or, if you prefer SSH:"
echo ""
echo "git remote add origin git@github.com:$GITHUB_USERNAME/$REPO_NAME.git"
echo "git branch -M main"
echo "git push -u origin main"
