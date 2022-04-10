package study.datajpa.entity;

import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;

@MappedSuperclass
@Getter
public class BaseEntity extends BaseTimeEntity{

    @Column(updatable = false)
    @CreatedBy
    private String createdBy;

    @LastModifiedBy
    private String lasModifiedBy;

}


