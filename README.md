ProtocolFiller
==============

This tool can help you fill in grades in exam protocols using data from an Excel file.

Quick guide
-----------

Download <http://cs.au.dk/~amoeller/protocolfiller/protocolfiller.jar>

Then run
```
java -jar protocolfiller.jar <protocol> <grades> [ <output> ] [ <align> ]
```
where

* `<protocol>` is the grade protocol PDF

* `<grades>` is your Excel file with grades

* `<output>` is the name of the generated PDF (`out.pdf` by default)

* `<align>` is an optional horizontal alignment of the grades being filled in (`0` by default)

Example:
```
java -jar protocolfiller.jar protocol.pdf grades.xlsx
```

Your Excel file should contain two columns (without headers): the first column contains the student ID numbers, and the second column contains the grades.

The grade protocol PDF must be the original one, not a bitmap generated from a scanner.

Authors
-------

- Mathias Schwarz
- [Anders Møller](mailto:amoeller@cs.au.dk?subject=protocolfiller)
