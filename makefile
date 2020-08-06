# defining complier, flag and VM
JFLAGS = -g
JC = javac
JVM = java

# providing targets for building .class files from .java files
.SUFFIXES: .java .class

# target entry for creating .class files from .java files. Uses suffixds rule
.java.class:
	$(JC) $(JFLAGS) $*.java

# macro for java source files
CLASSES = \
        Server.java \
        Client.java \
        History.java \
        ClientGui.java 

# Maco for running Server
MAIN = Server

# the default make target entry
default: classes

# for suffix replacement (.java with .class)
classes: $(CLASSES:.java=.class)

# for executing Server on port 8080
run: $(MAIN).class
	$(JVM) $(MAIN) 8080

# for cleaning the .class files after usage
clean:
	$(RM) *.class
