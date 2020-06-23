package li.earth.urchin.twic.app;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.URL;

public class Resources {

    public static URL find(Class<?> subjectClass, String name) throws FileNotFoundException {
        URL resource = subjectClass.getResource(name);
        if (resource == null) throw new FileNotFoundException(name);
        return resource;
    }

    public static InputStream open(Class<?> subjectClass, String name) throws FileNotFoundException {
        InputStream resource = subjectClass.getResourceAsStream(name);
        if (resource == null) throw new FileNotFoundException(name);
        return resource;
    }

}
