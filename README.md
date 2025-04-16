# AI DEMO --Spring AI
### An app base on Spring AI framework.

#### When building a database agent, there are some tedious tasks that need to be addressed. Fundamentally, we require three knowledge bases: DDL, Database Description, and Q->SQL mappings. The purpose of this project is to experiment with whether these components can be directly populated during the development process, generate vector storage, and annotate corresponding service methods for Q->SQL mappings to avoid generating inefficient SQL. Ultimately, the goal is to enable answering questions based on both document knowledge bases and database knowledge bases.

## Generate DataBase Description
### By using the annotaion @AiVectorize, we can generate the decription text，here's an example:
#### **Entity**
```java
@Table(name = "test_entity")
@AiVectorize(name = "entity for software testing(test_entity)",description = "test record store in this table",type = AiVectorize.AiVectorizeType.ENTITY)
public class TestEntity {
    @AiVectorize(name = "id",description = "primary key for test, auto increment",type = AiVectorize.AiVectorizeType.FIELDS)
    private Long id;
    @AiVectorize(name = "name",description = "name for test, used for identification",type = AiVectorize.AiVectorizeType.FIELDS)
    private String name;
    
    

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
```
#### **Test method**
```java
    public static void main(String[] args) {
        AiVectorize entityAnnotation = TestEntity.class.getAnnotation(AiVectorize.class);
        if (entityAnnotation != null) {
            String description = new AiVectorizeProcessor().generateFullDescription(TestEntity.class, entityAnnotation);
            System.out.println(description);
        } else {
            System.out.println("No AiVectorize annotation found on TestEntity.");
        }
    }
```
####  **Output**
```
###entity for software testing(test_entity)
test record store in this table, properties are as follows:
 - id: primary key for test, auto increment
 - name: name for test, used for identification
 
```
