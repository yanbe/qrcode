#!/bin/sh
RESPATH=res
CLASSPATH=classes
ENTRYPOINT=example.QRCodeDecoderCUIExample
FILENAMES=""
echo "Testing..."

for i in `find $RESPATH -name "*.jpg" -or -name "*.png" -or -name "*.gif" -or -name "*.JPG" -or -name "*.PNG"`
do
  FILENAMES="$FILENAMES $i"
done

java -classpath $CLASSPATH $ENTRYPOINT $FILENAMES 1>/dev/null

echo "Test finished."
