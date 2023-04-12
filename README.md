# Tripcode
A (somewhat) simple Java-Python hybrid abomination.

## What the hell is this?
It's a tripcode generator and scanner, that makes use of Python's memory efficiency and Java's threading and speed. \
It can generate tripcodes indiscriminately, or if given a pattern, scan for tripcodes matching said pattern, case insensitive.

## But why?
For fun, of course. I recently learned about the concept of tripcodes and figured I'd try my hand at a scanner, which soon became a generator too. \
This can be used for any context where some sort of pseudo-identification is desired in an otherwise anonymous environment.

## Do you plan on doing anything else with this?
Maybe adding a bit more customization, along with providing an executable jar right here. This is already package-ready though, if you have Maven. \
In the event you want to package it yourself, all you'll need is at least Java 11 and Maven. Run `mvn assembly:assembly` from the `tripcode` directory,
then look for the jar in the `tripcode/target` directory. It's the one ending in `jar-with-dependencies.jar`.

## Author
Me, Yuriel. \
Do what you will to this, I wash my hands.