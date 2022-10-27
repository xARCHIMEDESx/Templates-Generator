# Description
## Main purpose
`templates-generator` is a Java-based application that renders text-content files based on templates and input data.
It uses [Apache Velocity](https://velocity.apache.org/) template engine as a core.


## Usage
### CLI mode
`templates-generator` has three mandatory and one optional arguments:
- *--template (-t)* - path to input template (.vm file).
- *--variables (-v)* - comma-separated list of paths to files/directories with variables. _.json_, _.yaml_/_.yml_ extensions are supported.
- *--output (-o)* - path to output directory where rendering result will be saved.
- *--combine (-c)* - No args. Optional. Default is _false_. Whether to combine multiple variables files' content inside single context.

The util can be launched via `java -jar` command with mentioned args as a CLI tool or even from Maven lifecycle.

_Here is an example_:

In `<dependencies\>` block you need to add the dependency
```xml
<dependency>
    <groupId>com.xarchimedesx</groupId>
    <artifactId>templates-generator</artifactId>
    <version>${templates-generator.version}</version>
</dependency>
```
and then launch it at needed phase using `exec-maven-plugin` in `<build\>` section:
```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.codehaus.mojo</groupId>
            <artifactId>exec-maven-plugin</artifactId>
            <version>${exec-maven-plugin.version}</version>
            <executions>
                <execution>
                    <id>run templates-generator</id>
                    <phase>compile</phase>
                    <goals>
                        <goal>java</goal>
                    </goals>
                    <configuration>
                        <mainClass>com.xarchimedesx.templatesgenerator.TemplatesGenerator</mainClass>
                        <arguments>
                            <argument>--template</argument>
                            <argument>path/to/*.vm</argument>
                            <argument>--variables</argument>
                            <argument>path/to/*.json|*.yaml|*.yml,path/to/dir/with/such/files</argument>
                            <argument>--output</argument>
                            <argument>path/where/output/should/be/saved</argument>
                            <argument>-combine</argument>
                        </arguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build> 
```

### Library mode
`templates-generator` also can be used as a regular Java library. Just add the dependency to the `<dependencies\>` block
in your `pom.xml`, create an object of `TemplatesGenerator` class and call the `render(...)` method on it passing required arguments.
```
TemplatesGenerator templatesGenerator = new TemplatesGenerator();
templatesGenerator.render(templatePath, variablesPaths, outputDirBasePath, isCombined);
```

## Variables files
**Variables file** can be any valid *.json* or *.yaml/.yml* files with any data, which is needed for templates.

*Example:*
```yaml
users: 
  - 
    id: 001
    group_id: 2
    personal: 
      age: 26
      name: John
      surname: Doe
  - 
    id: 002
    group_id: 1
    personal: 
      age: 28
      name: Jane
      surname: Doe
```
`templates-generator` merges provided templates with variables file, so they become accessible as VTL variables:
`$users.get(0).id`, `$users.get(1).personal.name` and so on.

Moreover, `templates-generator` supports not only files, but directories, where such files can be located.
In case of directory it recursively reads its content, filters supported files based on their extension and processes them one by one. 

## Combine mode
By default `templates-generator` works in **non-combined mode**, when for every _variables' file_, discovered in provided path, 
it renders _separate_ output. That means that for every such file a new Velocity context is created and merged with the template provided.
It can be useful when, for instance, we have a directory with several variables files, and we want to execute rendering 
against the same template for _each of them_ in one run.

When we use the `--combine` CLI option or directly pass _isCombined_ argument in the _render(...)_ method with the value `true`,
the **combined** mode is enabled. As it's said in its name, it combines all variables files' content into one big context and then
rendering is executed using this combined set of variables.
This is useful when we have variables from different sources, located in different files/directories, but all of them should be present in the context
during rendering.

The only **tricky part** here is what happens if two or more files have same data _"objects"_. Say, two .yaml's with
`users` lists in it. In this case those lists will be _encapsulated in separate sublists_ of the `users` VTL variable,
so you will have to reflect that in your template.
Like `$users.get(0).get(0).id` or flatten somehowe the data structure. 
Here is the example of flattening: [#macro( flattenUsers )](src/test/resources/templates/macros.vm).

## Velocity templates
**Velocity templates** are files (mostly with *.vm* extension) which contain the template of desired file to be rendered, written on VTL (Velocity Template Language)
or any other VTL code.
More details how to write templates can be found in this [guide](https://velocity.apache.org/engine/2.2/user-guide.html).

For `templates-generator` you need to provide a path to the *"main"* template, which can include calls to some other templates.

*Example*:
```
#set( $comma = ',' )
{
  "usersAmount": $users.size(),
  "users": [
    #foreach( $user in $users )
    {
      "ID": $user.id,
      "Group ID": $user.group_id,
      "Name": "$user.personal.name",
      "Surname": "$user.personal.surname",
      "Age": $user.personal.age
    }#if($foreach.hasNext)$comma#end
    #end
  ]
}
```

## Saving files
To save files `templates-generator` provides **custom VTL block directive**: `#saveFile`.
It supports **single argument - output file path** and the content of the file should be in its body.
You can provide the content directly as template, call a macro, parse another .vm or anything else valid from VTL point of view.

*Full example of template is following*:
```
#set( $comma = ',' )
#saveFile( "${outputDirBasePath}/test-data/users.json" )
{
  "usersAmount": $users.size(),
  "users": [
    #foreach( $user in $users )
    {
      "ID": $user.id,
      "Group ID": $user.group_id,
      "Name": "$user.personal.name",
      "Surname": "$user.personal.surname",
      "Age": $user.personal.age
    }#if($foreach.hasNext)$comma#end
    #end
  ]
}
#end
```
**$outputDirBasePath** - is actually the argument, passed to `templates-generator` as *--output*, and it is propagated to Velocity context.
Actually, you may not use it and pass only `test-data/users.json` to the save directive, but in this case file will be saved using relative path from the directory
in which `templates-generator` was launched. Using *$outputDirBasePath* gives possibility to save files at any location using absolute or some specific relative path.
You also may hardcode an absolute path directly in a template, but it is **strongly unrecommended**.

In case of _two or more variables files_ processed with **disabled** combined mode, subdirectories inside *$outputDirBasePath*
will be created. Names of those subdirectories will be delivered from original files' names that were processed.

To summarize, if value of *$outputDirBasePath* is *C\\:Users\\<user_name>\Desktop* a file will be created at *C\\:Users\\<user_name>\Desktop\test-data\users.json*
with following content:
```json
{
  "usersAmount": 2,
  "users": [
    {
      "ID": 1,
      "Group ID": 2,
      "Name": "John",
      "Surname": "Doe",
      "Age": 26
    },
    {
      "ID": 2,
      "Group ID": 1,
      "Name": "Jane",
      "Surname": "Doe",
      "Age": 28
    }
  ]
}
```

## VelocityTools
**VelocityTools** is a set of useful classes which help to write templates.
More detailed info about them can be found [here](https://velocity.apache.org/tools/3.1/) and [here](https://velocity.apache.org/tools/3.1/tools-summary.html).
`templates-generator` provides support for **GenericTools** only.
#### Custom VelocityTools
`templates-generator` implements one custom tool - `ExceptionTool`.
It provides possibility for a developer to throw an exception during rendering. For example, when some variables in input file are invalid, do not meet some limitations, etc.

**Usage:** `$exception.throwRenderingException(<exception_message>)`.

An instance of a custom *RenderingException* which is extended from *VelocityException* will be thrown and rendering process halted.
