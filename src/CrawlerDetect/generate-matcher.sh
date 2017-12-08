#

# Get the directory in which this script resides.

SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )

# There are two input files: exclusions and crawlers, which
# must be provided in that order.
awk -f $SCRIPTPATH/generate-matcher.awk \
    $SCRIPTPATH/orig/Exclusions.php \
    $SCRIPTPATH/orig/Crawlers.php
