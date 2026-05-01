## Description

New drive components created in the authoring environemnt do not apply force in the correct direction.

## Expected Behavior

The expected behavior is that the drive component should apply force in the direction specified in the authoring environment, but relative to the object's orientation (see below for more context). So if the object is flipped upside down, the drive component should apply force in the direction (0, 1) (with authoring direction (0, -1)).
## Current Behavior

After creating a new drive component in the authoring environment, the drive component applies force in the direction specified in the authoring environment, instead of applying force in this direction, but relative to the object's orientation. That is, if the direction in the authoring environment was (0, -1), the drive component applies force in the direction (0, -1) even when the object is flipped upside down. 

## Steps to Reproduce

Provide detailed steps for reproducing the issue.

 1. Go to the authoring environment and create a 
 1. step 2
 1. and so on

## Failure Logs


## Hypothesis for Fixing the Bug

The test will simply instantiate a component with a certain external force, e.g. (100, 0). Then it will rotate the body by 90 degrees. When the force is applied, we expect to read a total force vector of (0, 100). The creation of new drive components does not actually instantiate new drive components, but rather uses the external forces mechanism on plain dynamic components. These forces are are applied only when the button is pressed, and should be rotated to the orientation of the object when they are added to the total force vector in DynamicComponentUtilities.java:68.
