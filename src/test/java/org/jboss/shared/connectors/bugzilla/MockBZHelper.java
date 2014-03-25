package org.jboss.shared.connectors.bugzilla;

import java.util.HashMap;

import org.jboss.pull.shared.connectors.bugzilla.BZHelper;
import org.jboss.pull.shared.connectors.bugzilla.Bug;

public class MockBZHelper extends BZHelper {

    public MockBZHelper() {
    }
    
    @Override
    public Bug getBug(Integer id){
        HashMap<String, Object> bugMap = new HashMap<String, Object>();
        bugMap.put("id", id);
        bugMap.put("alias", new String[]{});
        bugMap.put("product", "Jboss Enterprise Application Platform 6");
        bugMap.put("component", new String[]{});
        bugMap.put("version", new String[]{});
        bugMap.put("priority", "");
        bugMap.put("severity", "");
        bugMap.put("target_milestone", "---");
        bugMap.put("target_release", new String[]{"6.2.4","5.1.0"});
        bugMap.put("creator", "");
        bugMap.put("assigned_to", "");
        bugMap.put("qa_contact", "");
        bugMap.put("docs_contact", "");
        bugMap.put("status", "NEW");
        bugMap.put("resolution", "");
        bugMap.put("flags", new String[]{});
        bugMap.put("groups", new String[]{});
        bugMap.put("depends_on", new String[]{});
        bugMap.put("blocks", new String[]{});
        bugMap.put("summary", "");
        bugMap.put("description", "");
        
        return new Bug(bugMap);
    }
    
    public boolean updateBugzillaStatus(Integer bugzillaId, Bug.Status status) {
        return false;
    }
}
