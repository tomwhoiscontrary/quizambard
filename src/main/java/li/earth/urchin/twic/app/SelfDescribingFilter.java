package li.earth.urchin.twic.app;

import com.sun.net.httpserver.Filter;

public abstract class SelfDescribingFilter extends Filter {

    @Override
    public String description() {
        return getClass().getSimpleName();
    }

}
