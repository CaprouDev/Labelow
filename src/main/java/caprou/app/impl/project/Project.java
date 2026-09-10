package caprou.app.impl.project;

import lombok.AllArgsConstructor;

import java.io.File;

@AllArgsConstructor
public class Project {

    private final File projectFile;
    private final String projectName;
    private final String projectDescription;
    private double completion;


}
