## Description

When the buttons are added in the authoring environment, they do not get displayed in the gameplay

## Expected Behavior

The buttons should appear in the gameplay and toggling them should apply action to components

## Current Behavior

Only the hardcoded buttons from the default components appear

## Steps to Reproduce

Provide detailed steps for reproducing the issue.

1. Start any game with the custom button
2. Button will not pop up.

## Failure Logs

## Hypothesis for Fixing the Bug

Rewiring the code to use the buttons we added in the authoring environment as opposed to just the buttons from the default building components chosen should fix the issue.
The way to test is is to see that all the appropriate buttons appear on the screen and the appropriate forces are applied to the car