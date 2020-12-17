##FDMCI_Moodle2qti converter

This program converts a .xml export file of a Moodle questionbank into a _qti.zip backup file that can be imported into TestVision.

###use
1. Export your question bank into the moodle .xml format (including context and subcategories as you wish)
2. Copy the .xml file into the src/main/resources folder
3. Edit FDMCIMoodle2QtiMain to include your file name into the MOODLE_SOURCES list (you can convert multiple sources in one run)
4. (Build clean and) Run FDMCIMoodle2QtiMain
5. Review the log of the conversion in the console output
6. Review the result of the conversion in target/classes. Here you find both the _qti.zip file and its expanded folder. 
   (TestVision only accepts a special way .zip compression, which is incorporated in the converter)
7. Import the _qti.zip file into TestVision.
   Check on the details of the error messages, if any, and ignore them, or fix them in the moodle source, or enhance the converter to deal with them.    
8. Try-out the imported questions in testvision, and move the imported categories to appropriate folder location as you find appropriate.
Also test whether you can also edit the questions from here onwards in test vision.
   
###Question types

Moodle question types covered are:
- Multiple Choice
- Short Answer
- Numerical
- Drag and Drop into Image
- Embedded Answer (Cloze)
- Essay
- Matching
- True/False

TestVision question types converted into are
- Een-uit-meer
- Meer-uit-meer
- Match (ongelijk)
- Hotmatch
- Invul
- Invul (numeriek)
- Invul (meervoudig)
- Open

###Other noteworthy features

1. The converter maintains the categories substructure of Moodle into the folders substructure of testvision
Images are converted to original file types (e.g. png or jpeg) and embedded into the target as is.

2. The Qti 2.1 standard is used for the target format.
The converter includes many transformations of the html format of questions and answers in Moodle towards xhtml compliant
specification, which is required by the Qti schemas. (Unlike Moodle, which stores its html content within !CDATA[ tags.)

3. The converter tracks duplicate exports of images and tries to reconcile those into associated questions.
Sometimes moodle exports different images with the same name.
   
4. You can set the logging level in the Run Configuration of intellij
    -Dorg.slf4j.simpleLogger.defaultLogLevel=ERROR
   This manages level of verbosity at the console output.