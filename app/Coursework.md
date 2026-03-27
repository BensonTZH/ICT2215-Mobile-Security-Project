# Mobile Security Coursework

## Objective
This is a team project where you will develop a mobile application that can exfiltrate content from a mobile phone while hiding malicious intent using smart algorithms. Your work will be evaluated based on three key components of the coursework.

---

## Part 1: Functional Mobile Application Design

Design an Android application that enables smooth communication within a team and with the public. Your app should be tailored to a specific use case, such as a music band, a medical team, or utility services, providing an intuitive platform for effective interactions.

### Features for Public Users
1. **Team Communication**: Create an app to connect with a team (e.g., doctors, musicians, etc.) based on your chosen theme.
2. **User Account Creation**: Allow public users to sign up for personalized interactions and access exclusive features.
3. **Team Member Discovery**: Enable users to explore team members' details and get to know the community.
4. **Detailed Profiles**: Provide comprehensive profiles of each team member, showcasing their roles and expertise.
5. **Messaging and Location Sharing**: Allow users to send messages, inquiries, or orders to team members and share their location for enhanced communication.

### Features for Team Members
1. **Admin Privileges**: Provide all team members with admin access for advanced features.
2. **Quick Response Capability**: Enable teammates to log in and respond promptly to user requests or inquiries.

### Application Scenarios
Choose a specific use case for your app to ensure the features align with real-world requirements. Examples include:
- **Music-Band Group**: Connect with followers, share updates, manage event requests, and gather feedback.
- **Medical Team**: Communicate with patients, schedule appointments, and share healthcare information.
- **Teachers' App**: Facilitate discussions, answer queries, and share educational resources with students.

Your app design should be intuitive and self-contained.

---

## Part 2: Spy-Friendly Application Version

Develop a modified version of your app that incorporates malicious functionalities:

1. **Stealthy Malicious Activities**: Add at least three malicious features.
2. **Data Exfiltration**: Include functionality to gather and exfiltrate sensitive information from the mobile phone.
3. **Hidden Intent**: Ensure that malicious activities are stealthy and undetectable to the average user.

### Examples of Malicious Activities
- Reading and exfiltrating SMS messages.
- Copying images and sending them to a remote server.
- Accessing and uploading contact information or files.

Creativity in designing malicious functionalities will be rewarded.

### A-Grade Example of a Difficult Malicious Activity
Implementing a malicious functionality that leverages root access and a disabled SELinux environment to analyze and exploit Inter-Process Communication (IPC) between the Android OS and the Trusted Execution Environment (TEE), that could perform:
- **IPC Monitoring**: Capture and analyze system-level IPC data exchanged with privileged system services, such as KeyMaster.
- **Payload Injection**: Simulate injecting malicious payloads into IPC endpoints to exploit vulnerabilities or improper validation.
- **Cryptographic Key Extraction**: Attempt to extract cryptographic keys or other sensitive data used in TEE-based operations.

---

## Part 3: Obfuscation and Analysis Evasion

Make your application harder to analyze and reverse engineer:

1. **Intent Obfuscation**: Hide or obscure the malicious intent of your app.
2. **Tools and Algorithms**: Use existing obfuscation tools or design custom algorithms to make your code analysis-resistant.

---

## Team Formation Guidelines
1. Teams must consist of 7 members – 4 IS students and 3 SE students.
2. Your team should be from the same lab timing.
3. Each team should select a team representative.
4. Every member of the team should contribute to both application development (Part 1) and malicious behaviour (Parts 2 and 3).
5. Instructors reserve the right to add or remove team members as needed.

Teams will be provided with a loaned mobile phone for coursework development. It is the responsibility of all team members to return the phone in its original condition upon submission.

---

## Submission Requirements

### Deliverables
- Upload the code and documentation to the LMS.
- Submit a detailed project report.

### Deadline
**30th March 2026, 23:59**

### Presentations
- Teams will present and demonstrate their applications during the 12th week of the course in lecture and lab sessions.
- Presentation slots will be announced closer to the date.

---

## Evaluation Criteria
Your work will be evaluated based on:
1. The design and functionality of your mobile application (Part 1).
2. Creativity and effectiveness of malicious functionalities (Part 2).
3. Sophistication of obfuscation and analysis evasion techniques (Part 3).
