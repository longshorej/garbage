# file-open-penalty

## Overview

Measures the performance impact of opening a file that doesn't exist vs checking if it does first.

## Conclusion

Opening a file that doesn't exist takes twice as long (on ext4, 4.18.16-arch1-1-ARCH) as checking if it exists.
