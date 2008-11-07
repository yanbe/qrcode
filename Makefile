SOURCEPATH = src
CLASSPATH = classes
LIBPATH = lib
RESPATH = res
EXAMPLEPATH = example
OUTPATH = $(CLASSPATH)
LIBFILE = $(LIBPATH)/qrcode.jar
SRC = $(EXAMPLEPATH)/QRCodeDecoderCUIExample.java 

ENTRYPOINT=example.QRCodeDecoderCUIExample
ENTRYPOINT_GUI=example.QRCodeDecoderGUIExample

all: $(SRC)
	javac -d $(OUTPATH) -sourcepath $(SOURCEPATH) $<

test:
	./test_decoder.sh

clean:
	rm -r $(CLASSPATH)/*

run: all
	java -classpath $(CLASSPATH) $(ENTRYPOINT) $$1

rungui: all
	java -classpath $(CLASSPATH) $(ENTRYPOINT_GUI)

lib: all
	jar cvfe $(LIBFILE) $(ENTRYPOINT) -C $(OUTPATH) jp

bin: all
	jar cvfe ./bin/qrcode-cui.jar $(ENTRYPOINT) -C $(OUTPATH) .

