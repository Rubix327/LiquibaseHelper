package me.rubix327.liquibasehelper.settings;

import lombok.Getter;

@Getter
@SuppressWarnings("unused")
public class CbsAnnotation {

    private final String qualifiedName;
    private final String shortName;

    public CbsAnnotation(String qualifiedName, String shortName) {
        this.qualifiedName = qualifiedName;
        this.shortName = shortName;
    }

    public static class CbsDatamodelClass extends CbsAnnotation {
        public static final CbsDatamodelClass INSTANCE = new CbsDatamodelClass();
        public static final String QUALIFIED_NAME = "CbsDatamodelClass";
        public static final String SHORT_NAME = "CbsDatamodelClass";

        public CbsDatamodelClass() {
            super(QUALIFIED_NAME, SHORT_NAME);
        }

        public static class Fields {
            public static final String TAG = "tag";
            public static final String COMMENT = "comment";
            public static final String DESCRIPTION = "description";
            public static final String MAPPED = "mapped";
        }
    }

    public static class CbsDatamodelField extends CbsAnnotation {
        public static final CbsDatamodelField INSTANCE = new CbsDatamodelField();
        public static final String QUALIFIED_NAME = "CbsDatamodelField";
        public static final String SHORT_NAME = "CbsDatamodelField";

        public CbsDatamodelField() {
            super(QUALIFIED_NAME, SHORT_NAME);
        }

        public static class Fields {
            public static final String COMMENT = "comment";
            public static final String DESCRIPTION = "description";
            public static final String REQUIRED = "required";
            public static final String MAX_LENGTH = "maxLength";
            public static final String TYPE = "type";
            public static final String AVAILABLE_VALUES = "availableValues";
            public static final String AVAILABLE_VALUES_ENUM = "availableValuesEnum";
            public static final String AVAILABLE_VALUES_ENUM_PATH = "availableValuesEnumPath";
        }
    }

    public static class CbsDatamodelValue extends CbsAnnotation {
        public static final CbsDatamodelValue INSTANCE = new CbsDatamodelValue();
        public static final String QUALIFIED_NAME = "CbsDatamodelValue";
        public static final String SHORT_NAME = "CbsDatamodelValue";

        public CbsDatamodelValue() {
            super(QUALIFIED_NAME, SHORT_NAME);
        }

        public static class Fields {
            public static final String VALUE = "value";
        }
    }

    public static class CbsDatamodelIgnore extends CbsAnnotation {
        public static final CbsDatamodelIgnore INSTANCE = new CbsDatamodelIgnore();
        public static final String QUALIFIED_NAME = "CbsDatamodelIgnore";
        public static final String SHORT_NAME = "CbsDatamodelIgnore";

        public CbsDatamodelIgnore() {
            super(QUALIFIED_NAME, SHORT_NAME);
        }

        public static class Fields {
        }
    }

}
