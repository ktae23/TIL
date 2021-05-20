### Auto Complete Search

```html
<div class="search-box">
    <input type="text" placeholder="영화를 입력 후 Enter를 치세요" id= "autocompleteText" name="movieTitle" aria-label="Search">
    <div class="search-btn" id="searchBtn">
        <i class="fas fa-search"></i>		            		
    </div>
</div>
```

<br/>

#### autoComplete.js

```javascript
$(document).ready(function() {
	$( "#autocompleteText" ).autocomplete({
		source : function( request, response ) {
	        $.ajax({
	            url: "autoSearch",
	            dataType: "json",
	            data: {
	              searchValue: request.term
	            },
	            success: function( result ) {
	                response( 
	                	$.map( result, function( item ) {

	                			return {
	                  				label: item.data,
	                  				value: item.data
	                			}
	              		})
	              	);
	            }
	          });
	    },
		select: function( event, ui ) {},
        minLength: 1
	});
});
```

<br/>

#### Auto Search Controller

```java
@RequestMapping(value = "autoSearch", 
                method= {RequestMethod.GET},
                produces = "application/json; charset=utf8")
@ResponseBody
public void autoSearch(Model model,HttpServletRequest request,
                       HttpServletResponse response) throws IOException {
    
    JSONArray arrayObj = movieService.autoSearch(request, response);
    response.setCharacterEncoding("UTF-8");
    PrintWriter pw = response.getWriter(); 
    pw.print(arrayObj); 
    pw.flush(); 
    pw.close();

}
```

<br/>

#### MovieService

```java
public JSONArray autoSearch(HttpServletRequest request,
                            HttpServletResponse response) throws IOException {
    
    String searchValue = request.getParameter("searchValue"); 
    JSONArray arrayObj = new JSONArray();
    JSONObject jsonObj = null; 
    ArrayList<String> resultlist = new ArrayList<String>(); 
	
    // JPA 기능 사용, 포함 단어 검색 메서드인 findByBovieTitleContains();를 이용해도 괜찮음
    List<Movie> movies = movieRepository.findByMovieTitleStartsWith(searchValue, Sort.by(Sort.Direction.ASC, "movieTitle"));

    for(Movie movie : movies) { 
        String str = movie.getMovieTitle();
        resultlist.add(str); 
    } 
    //뽑은 후 json파싱 
    for(String str : resultlist) {
        jsonObj = new JSONObject();
        jsonObj.put("data", str);
        arrayObj.add(jsonObj); 
    } 

    return arrayObj;

}
```

<br/>

#### MovieRepository

```java
public interface MovieRepository extends JpaRepository<Movie, BigDecimal>{
	
    // ....
    public List<Movie> findByMovieTitleStartsWith(String searchValue, Sort sort);
}
```

