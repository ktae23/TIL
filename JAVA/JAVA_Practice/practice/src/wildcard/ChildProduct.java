package wildcard;

public class ChildProduct <T, M, C> extends ParentProduct<T, M>{

	private C company;

	public C getCompany() {
		return company;
	}

	public void setCompany(C company) {
		this.company = company;
	}
}