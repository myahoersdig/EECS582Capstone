/*
Filename: Users.java
Author(s): Abdelrahman Zeidan
Created: Mar 1
Last Modified:
Overview and Purpose: Model class used to store and manage user account data including id, first name, last name, email, and password.
Notes:
*/

/*
Class Name: Users
Description of Class Purpose/Function: This class represents a user object and provides getter and setter methods for user-related data.
*/
package com.example.eecs582capstone;

/*
Filename: Users.java
Author(s): Abdelrahman Zeidan
Created: 03-01-2026
Last Modified: 03-01-2026
Overview and Purpose: 
Notes: 
*/


/*
Users class: 
*/

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
