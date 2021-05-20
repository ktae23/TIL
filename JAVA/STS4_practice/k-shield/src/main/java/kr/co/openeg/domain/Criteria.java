package kr.co.openeg.domain;

import org.springframework.web.util.UriComponentsBuilder;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@ToString
@Setter
@Getter
public class Criteria {

	private static final int AMOUNT_OF_PAGE_NUMBERS = 10;
	
	private int pageNum; // 현재 페이지 번호
	private int amount;  // 표시할 페이지 번호의 개수

	private String type;
	private String keyword;

	public Criteria() {
		this(1, AMOUNT_OF_PAGE_NUMBERS);
	}

	public Criteria(int pageNum, int amount) {
		this.pageNum = pageNum;
		this.amount = amount;
	}

	public String[] getTypeArr() {
		return type == null ? new String[] {} : type.split("");
	}
	
	public int getPageStart() {
		return (this.pageNum-1) * amount;
	}
	
	public int getPerPageNum(){
		return this.amount;
	}
	
	public String getListLink() {
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath("")
				.queryParam("pageNum", this.pageNum     )
				.queryParam("amount" , this.getAmount ())
				.queryParam("type"   , this.getType   ())
				.queryParam("keyword", this.getKeyword());
		return builder.toString();
	}
}
