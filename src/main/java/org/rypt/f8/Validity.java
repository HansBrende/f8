package org.rypt.f8;

public enum Validity {

    SINGLE_BYTES {
        @Override
        public boolean isAscii() {
            return true;
        }

        @Override
        public boolean isFullyValid() {
            return true;
        }

        @Override
        public boolean isValidOrTruncated() {
            return true;
        }
    },
    MULTI_BYTES {
        @Override
        public boolean isAscii() {
            return false;
        }

        @Override
        public boolean isFullyValid() {
            return true;
        }

        @Override
        public boolean isValidOrTruncated() {
            return true;
        }
    },
    UNDERFLOW_R1 {
        @Override
        public boolean isAscii() {
            return false;
        }

        @Override
        public boolean isFullyValid() {
            return false;
        }

        @Override
        public boolean isValidOrTruncated() {
            return true;
        }
    },
    UNDERFLOW_R2 {
        @Override
        public boolean isAscii() {
            return false;
        }

        @Override
        public boolean isFullyValid() {
            return false;
        }

        @Override
        public boolean isValidOrTruncated() {
            return true;
        }
    },
    UNDERFLOW_R3 {
        @Override
        public boolean isAscii() {
            return false;
        }

        @Override
        public boolean isFullyValid() {
            return false;
        }

        @Override
        public boolean isValidOrTruncated() {
            return true;
        }
    },
    MALFORMED {
        @Override
        public boolean isAscii() {
            return false;
        }

        @Override
        public boolean isFullyValid() {
            return false;
        }

        @Override
        public boolean isValidOrTruncated() {
            return false;
        }
    };

    public abstract boolean isAscii();

    public abstract boolean isFullyValid();

    public abstract boolean isValidOrTruncated();

}
