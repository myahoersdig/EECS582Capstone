package com.example.eecs582capstone;

public class Users {
    private int id;
    private String firstname;
    private String lastname;
    private String emailAddress;
    private String Password;

    public Users(int id, String firstname, String lastname, String emailAddress, String password) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.emailAddress = emailAddress;
        Password = password;
    }

    // Getters and setters (unchanged)
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstname() { return firstname; }
    public void setFirstname(String firstname) { this.firstname = firstname; }
    public String getLastname() { return lastname; }
    public void setLastname(String lastname) { this.lastname = lastname; }
    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }
    public String getPassword() { return Password; }
    public void setPassword(String password) { Password = password; }
}