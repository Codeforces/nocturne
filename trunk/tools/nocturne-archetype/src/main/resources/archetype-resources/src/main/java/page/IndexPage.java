package ${packageName}.page;

import org.nocturne.link.Link;
import org.nocturne.main.Page;

/** 
 * Simple controller, which just adds the single
 * variable into view layer.
 */
@Link("")
public class IndexPage extends Page {
    @Override
    public void action() {
        put("name", "world");
    }
}
