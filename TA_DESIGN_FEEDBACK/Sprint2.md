## Design

Design is pretty good overall a few things could be improved, however, there are still improvements that could be made. For instance, in the authoring environment, it would be cleaner if the ToolSelectorImpl class is a record instead given that it solely consists of a setter and getter. Also don't think that the screen registration should be in main given that there is already a ScreenManager object inside of ScreenFactory. The constructor in the AuthoringEntryScreenImpl class also has too many responsibilities at the moment, should be extracted as seperate methods to improve readability. The same thing could also be said for the AuthoringScreenImpl class. Also avoid protected methods/variables

## Testing

Good mix of positive and negative tests and they are simple and don't do too much. However, they still need to implement 1) more tests and 2) implement tests for a feature soon after said feature is implemented and not put off testing. Note that you need 2 tests per public method.

## Agile Process

Additionally, the agile process looks solid, great job using proper commits and commit sizes. Just make sure to close your git issues when done.