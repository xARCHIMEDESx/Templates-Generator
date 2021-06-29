# Description
## Main purpose
`templates-generator` is a Java-based application that renders text-content files basing on templates and input data.
It uses [Apache Velocity](https://velocity.apache.org/) template engine as a core.


## Usage
`templates-generator` has three mandatory arguments:
- *--template (-t)* - path to input template (.vm file).
- *--variables (-v)* - path to file with variables (.json, .yaml/.yml are supported).
- *--output (-o)* - path to output directory where rendering result will be saved.

The util can be launched via `java -jar` command with mentioned args or even from Maven lifecycle.

_Here is an example_:

In `\<dependencies\>` block you need to add the dependency
```xml
<dependency>
    <groupId>com.xarchimedesx</groupId>
    <artifactId>templates-generator</artifactId>
    <version>${templates-generator.version}</version>
</dependency>
```
and then launch it at needed phase using `exec-maven-plugin` in `\<build\>` section:
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
                            <argument>path/to/*.json|*.yaml|*.yml</argument>
                            <argument>--output</argument>
                            <argument>path/where/output/should/be/saved</argument>
                        </arguments>
                    </configuration>
                </execution>
            </executions>
        </plugin>
    </plugins>
</build> 
```


## Variables file
**Variables file** can be any valid *.json* or *.yaml/.yml* files with any data, which is needed for templates.

*Example:*
```yaml
users: 
  - 
    id: 001
    personal: 
      age: 26
      name: John
      surname: Doe
  - 
    id: 002
    personal: 
      age: 28
      name: Jane
      surname: Doe
```
`templates-generator` merges provided templates with variables file, so they become accessible as VTL variables:
`$users.get(0).id`, `$users.get(1).personal.name` and so on.

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
      "ID": "$user.id",
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
#saveFile( "${outputDirBasePath}/test-data/rendered.json" )
#set( $comma = ',' )
{
  "usersAmount": $users.size(),
  "users": [
    #foreach( $user in $users )
    {
      "ID": "$user.id",
      "Name": "$user.personal.name",
      "Surname": "$user.personal.surname",
      "Age": $user.personal.age
    }#if($foreach.hasNext)$comma#end
    #end
  ]
}
#end
```
**$outputDirBasePath** - is actually the argument, passed to `templates-generator` as *--output* and it is propagated to Velocity context.
Actually, you may not use it and pass only `test-data/rendered.json` to the save directive, but in this case file will be saved using relative path from the directory
in which `templates-generator` was launched. Using *$outputDirBasePath* gives possibility to save files at any location using absolute or some specific relative path.
You also may hardcode an absolute path directly in a template but it is **strongly unrecommended**.

To summarize, if value of *$outputDirBasePath* is *C\\:Users\\<user_name>\Desktop* a file will be created at *C\\:Users\\<user_name>\Desktop\test-data\rendered.json*
with following content:
```json
{
  "usersAmount": 2,
  "users": [
    {
      "ID": "1",
      "Name": "John",
      "Surname": "Doe",
      "Age": 26
    },
    {
      "ID": "2",
      "Name": "Jane",
      "Surname": "Doe",
      "Age": 28
    }
  ]
}
```

## VelocityTools
**VelocityTools** is a set of useful classes which help to write templates.
More detailed info about them can be found [here](https://velocity.apache.org/tools/3.0/) and [here](https://velocity.apache.org/tools/3.0/tools-summary.html).
`templates-generator` provides support for **GenericTools** only.
#### Custom VelocityTools
`templates-generator` implements one custom tool - `ExceptionTool`.
It provides possibility for a developer to throw an exception during rendering. For example, when some variables in input file are invalid, do not meet some limitations, etc.

**Usage:** `$exception.throwRenderingException(<exception_message>)`.

An instance of a custom *RenderingException* which is extended from *VelocityException* will be thrown and rendering process halted.
