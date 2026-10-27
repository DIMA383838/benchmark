package academy;

public record Student(String name, int age) {
    public String getName() {
        return name;
    }
}
