package generic_practice;

public class GenericPrinter<T> {
	private T material;
	
	public void setMaterial(T material) {
	this.material = material;
	}
	
	public T getMaterial(){
	return material;
	}
}