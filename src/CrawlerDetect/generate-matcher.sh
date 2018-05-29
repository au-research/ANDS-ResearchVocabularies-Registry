#

# Get the directory in which this script resides.

SCRIPTPATH=$( cd $(dirname $0) ; pwd -P )

# NB: gawk 4 is required! If your default "awk" is not
# gawk 4 (or later), set the GAWK environment variable
# to point to an executable that _is_ gawk 4.
: ${GAWK:=awk}

# There are two input files: exclusions and crawlers, which
# must be provided in that order.
${GAWK} -f $SCRIPTPATH/generate-matcher.awk \
    $SCRIPTPATH/orig/Exclusions.php \
    $SCRIPTPATH/orig/Crawlers.php
