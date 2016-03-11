Aphrodite
===========
An API for retrieving and updating SET issues from multiple issue trackers.

#Configuration
------------
Add aphrodite to your pom:
```maven
    <dependency>
      <groupId>org.jboss.set</groupId>
      <artifactId>aphrodite</artifactId>
      <version>0.3.0</version>
    </dependency>
```
Add the remote repository to your pom:
```maven
	<repositories>
        <repository>
            <id>aphrodite</id>
            <name>Aphrodite</name>
            <url>
            	https://repository.jboss.org/nexus/content/groups/public/
            </url>
            <layout>default</layout>
            <releases>
                <enabled>true</enabled>
                <updatePolicy>never</updatePolicy>
            </releases>
            <snapshots>
                <enabled>true</enabled>
                <updatePolicy>never</updatePolicy>
            </snapshots>
        </repository>
   </repositories>
   
```

##### Configuring via json file
Specify the location of the aphrodite.properties.json file via the system property "aphrodite.config". An example properties file can be found [here](https://github.com/jboss-set/aphrodite/blob/master/aphrodite.properties.json.example)
```java
Aphrodite aphrodite = Aphrodite.instance();
```

##### Configuring programmatically
```java
IssueTrackerConfig jiraService =
                new IssueTrackerConfig("https://issues.stage.jboss.org", "your username", "your password", "jira", 200);
List<IssueTrackerConfig> issueTrackerConfigs = new ArrayList<>();
issueTrackerConfigs.add(jiraService);

RepositoryConfig githubService = new RepositoryConfig("https://github.com/", "your username", "your password", "github");
List<RepositoryConfig> repositoryConfigs = new ArrayList<>();
repositoryConfigs.add(githubService);


AphroditeConfig config = new AphroditeConfig(issueTrackerConfigs, repositoryConfigs);
Aphrodite aphrodite = Aphrodite.instance(config);
```
##### Warning
the class of Aphrodite implements the interface AutoCloseble,so you can call the close() method close the resource  after Aphrodite use has finished,
 or that Aphrodite.Instance() should be performed in a try with resources statement.for example:
 ```java
 
 try(Aphrodite aphrodite = Aphrodite.instance(config)){
 
 }
 
// or 
 
 aphrodite.close();
 ```
## Example Usage
------------
##### jira example
```java

// 1.Get individual Issue include comments
Issue issue = aphrodite.getIssue(new URL("https://issues.stage.jboss.org/browse/WFLY-100"));

// 2.Update issue
issue.setAssignee("ryanemerson");
aphrodite.updateIssue(issue);

// 3.Get issues
Collection<URL> urls=new ArrayList<>();
urls.add(new URL("https://issues.stage.jboss.org/browse/WFLY-4816"));
urls.add(new URL("https://issues.stage.jboss.org/browse/WFLY-4817"));
urls.add(new URL("https://issues.stage.jboss.org/browse/WFLY-100"));
List<Issue> issues=aphrodite.getIssues(urls);

// 4.Add comment to issue
aphrodite.addCommentToIssue(issue, new Comment(null,null,"comment test",false));

// 5.Add comments to issues
Issue issue2 = aphrodite.getIssue(new URL("https://issues.stage.jboss.org/browse/WFLY-100"));
Map<Issue,Comment> maps=new HashMap<>();
maps.put(issue, new Comment("comment test",false));
maps.put(issue2, new Comment("comment test",false));
aphrodite.addCommentToIssue(maps);

// 6.Search Issues
SearchCriteria sc = new SearchCriteria.Builder()
        .setStatus(IssueStatus.MODIFIED)
        .setProduct("JBoss Enterprise Application Platform 6")
        .build();
List<Issue> result = aphrodite.searchIssues(sc);

```
##### bugzila example
```java
//it's same as the jira test,you only need change the URL

// 1.Get individual issue include comments
Issue issue = aphrodite.getIssue(new URL("https://bugzilla.redhat.com/show_bug.cgi?id=1184440"));

// 2.Update issue
issue.setStatus(IssueStatus.ASSIGNED);
aphrodite.updateIssue(issue);
```
##### github example
```java
// 1.Get code repository
Repository repo=aphrodite.getRepository(new URL("https://github.com/ryanemerson/aphrodite_test"));

// 2.Get individual patch
Patch patch=aphrodite.getPatch(new URL("https://github.com/ryanemerson/aphrodite_test/pull/1"));

// 3.Get all patches associated with a given issue
List<Patch> patches=aphrodite.getPatchesAssociatedWith(new Issue(new URL("https://issues.jboss.org/browse/WFLY-100")));

// 4.Get patches by status
List<Patch> patches=aphrodite.getPatchesByStatus(repo, PatchStatus.CLOSED);

// 5.Add a comment to patch
aphrodite.addCommentToPatch(patch, "Example Comment");

// 6.Add label to patch,the label name must can be found in the patch
aphrodite.addLabelToPatch(patch, "bug");

// 7.Find patches related with the patch
List<Patch> patches=aphrodite.findPatchesRelatedTo(patch);

```

# Contributing
------------
Contributions welcome, but make sure your code passes checkstyle and respects the [formatting style](https://github.com/jboss-set/aphrodite/blob/master/ide-configs/eclipse/formatter.xml) before submitting a PR.  Furthermore, all new files must contain the JBOSS copyright notice, templates for different IDEs can be found [here](https://github.com/jboss-set/aphrodite/tree/master/ide-configs).
