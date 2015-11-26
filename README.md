Aphrodite
===========
An api for retrieving and updating SET issues from multiple issue trackers.

#Configuration
------------
Add aphrodite to your pom:
```maven
    <dependency>
      <groupId>org.jboss.set</groupId>
      <artifactId>aphrodite</artifactId>
      <version>0.1.1</version>
    </dependency>
```

##### Configuring via json file
Specify the location of the aphrodite.properties.json file via the sytem property "aphrodite.config". An example properties file can be found [here](https://github.com/jboss-set/aphrodite/blob/master/aphrodite.properties.json.example)
```java
Aphrodite aphrodite = Aphrodite.instance();
```

##### Configuring programmatically
```java
IssueTrackerConfig jiraService =
                new IssueTrackerConfig("https://issues.stage.jboss.org", "username", "password", "jira", 200);
List<IssueTrackerConfig> issueTrackerConfigs = new ArrayList<>();
issueTrackerConfigs.add(jiraService);

RepositoryConfig githubService = new RepositoryConfig("https://github.com/", "username", "password", "github");
List<RepositoryConfig> repositoryConfigs = new ArrayList<>();
repositoryConfigs.add(githubService);

AphroditeConfig config = new AphroditeConfig(issueTrackerConfigs, repositoryConfigs);
Aphrodite aphrodite = Aphrodite.instance(config);
```

## Example Usage
------------
```java
// Search Issues
SearchCriteria sc = new SearchCriteria.Builder()
        .setStatus(IssueStatus.MODIFIED)
        .setProduct("JBoss Enterprise Application Platform 6")
        .build();
List<Issue> result = aphrodite.searchIssues(sc);
System.out.println(result);

// Get individual Issue
Issue issue = aphrodite.getIssue(new URL("https://issues.stage.jboss.org/browse/WFLY-100"));

// Update issue
issue.setAssignee("ryanemerson");
aphrodite.updateIssue(issue);

// Get individual Patch
Patch patch = aphrodite.getPatch(new URL("https://github.com/ryanemerson/aphrodite_test/pull/1"));

// Get code repository
Repository repository = aphrodite.getRepository(new URL("https://github.com/ryanemerson/aphrodite_test"));

// Get all patches associated with a given issue
List<Patch> patches = aphrodite.getPatchesAssociatedWith(issue);

// Get patches based upon their status e.g. open PRs
patches = aphrodite.getPatchesByStatus(repository, PatchStatus.OPEN);

// Add a comment to a patch
aphrodite.addCommentToPatch(patch, "Example Comment");
```
