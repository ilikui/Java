package info.likui.CH001;

public class Constants {

    public static void main(String[] args) {
        final double CM_PER_INCH = 2.54;
        double paperWidth =8.5;
        double paperHeight = 11;
        System.out.printf("Paper size in centimeters:"+paperHeight*CM_PER_INCH+" by "+paperHeight*CM_PER_INCH);
    }
}
