# Bug Report: Settings Button Has No Action Handler

## Description

The "Settings" button on the game menu screen is non-functional. It is rendered in view, but clicking it produces no observable response.

## Expected Behavior

Clicking the Settings button should produce a settings UI where the user can adjust application-level preferences, such as theme or language.

## Current Behavior

Clicking the Settings button does nothing. The user cannot tell whether the click was registered.

## Steps to Reproduce

1. Launch the application.
2. Login and go to the main menu screen
3. Click the "Settings" button in the top-left button column.

## Failure Logs

No logs are produced when clicking the Settings button. JavaFX runtime silently drops the click event when no handler is registered.

## Hypothesis for Fixing the Bug

The issue is that `settingsButton` is never wired to an `onAction` handler. The fix is to call `settingsButton.setOnAction(...)` in game menu screen, passing in a handler that opens a settings dialog. This will allow the button to respond to clicks.
