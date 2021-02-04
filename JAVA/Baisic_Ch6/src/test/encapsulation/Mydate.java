package test.encapsulation;

public class Mydate {

		private int year,month,day;

		public int getYear() {
			return year;
		}
		
		void setYear(int year2){
			//유효성 검사
			if(year2>0 && year2<2022) {
				year=year2;
			}else{
				System.out.println("잘못된 입력입니다.");
			}			
				
		}
		
		public int getMonth() {
			return month;
		}
		
		public void setMonth(int month) {
			//유효성 검사
			if(month>0 && month<13) {
				this.month = month;
			}
			else {
				System.out.println("잘못된 입력입니다.");
			}
		}	

		public int getDay() {
			return day;
		}			
		public void setDay(int day) {
			//유효성 검사
			if(day>0 && day<32) {
				this.day = day;
			}
			else {
				System.out.println("잘못된 입력입니다.");
			}			
	
		}

}
