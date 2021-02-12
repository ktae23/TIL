package wildcard;

public class ParentProduct <T, M>{

	private T kind;
	private M model;
	
	public T getKind() {
		return kind;
	}
	public M getModel() {
		return model;	
	}
	
	public void setKind(T kind) {
		this.kind = kind;
	}
	public void setModel(M model) {
		this.model = model;
	}
}
	class Tv{
		String tv = "tv";
		
		public String getTv() {
			return tv;
		}
	}
