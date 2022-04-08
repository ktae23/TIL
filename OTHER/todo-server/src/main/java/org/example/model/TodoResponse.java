package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TodoResponse {

    private Long id;
    private String title;
    private Long order;
    private Boolean compoleted;
    private String url;


    public TodoResponse(TodoModel todoModel){
        this.id = todoModel.getId();
        this.order= todoModel.getOrder();
        this.title= todoModel.getTitle();
        this.compoleted = todoModel.getCompleted();
        
        this.url= "http://localhost:8080/" + this.id;
    }


}
