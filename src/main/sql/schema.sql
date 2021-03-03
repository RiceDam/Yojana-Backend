DROP DATABASE IF EXISTS project_management_system;
CREATE DATABASE project_management_system;

CREATE USER IF NOT EXISTS 'admin'@'localhost' IDENTIFIED BY 'admin';
CREATE USER IF NOT EXISTS 'admin'@'%' IDENTIFIED BY 'admin';
GRANT ALL ON project_management_system.* TO 'admin'@'localhost';
GRANT ALL ON project_management_system.* TO 'admin'@'%';

USE project_management_system;
DROP TABLE IF EXISTS PayGrade;
CREATE TABLE PayGrade(
	LabourGrade VARCHAR(4),
    ChargeRate FLOAT(5,2),
    PRIMARY KEY(LabourGrade)
);

DROP TABLE IF EXISTS Employee;
CREATE TABLE Employee(
    EmpID INT NOT NULL UNIQUE AUTO_INCREMENT,
    EmpName VARCHAR(50) NOT NULL,
    LabourGrade VARCHAR(4),
    CreatedBy VARCHAR(255),
    ManagedBy VARCHAR(255),
    TimesheetApproverID VARCHAR(255),
    ProfileImage TINYTEXT,
    IsHR BOOLEAN,
    IsAdmin BOOLEAN,
    IsProjectManager BOOLEAN,
    IsTimesheetApprover BOOLEAN,
    CONSTRAINT PKEmployee PRIMARY KEY (EmpID),
    CONSTRAINT FKEmployeeLabourGrade
		FOREIGN KEY (LabourGrade)
			REFERENCES PayGrade(LabourGrade),
	CONSTRAINT FKEmployeeCreatedBy
		FOREIGN KEY (CreatedBy)
			REFERENCES Employee(EmpID),
	CONSTRAINT FKEmployeeManagedBy
		FOREIGN KEY (ManagedBy)
			REFERENCES Employee(EmpID),
    CONSTRAINT FKEmployeeTimesheetApproverID
		FOREIGN KEY (TimesheetApproverID)
			REFERENCES Employee(EmpID)
);

DROP TABLE IF EXISTS Credential;
CREATE TABLE Credential(
    EmpID INT NOT NULL UNIQUE,
	EmpUserName VARCHAR(10) NOT NULL UNIQUE,
    EmpPassword VARCHAR(15) NOT NULL,
	CONSTRAINT PKCredentialEmpID PRIMARY KEY (EmpID),
    CONSTRAINT FKCredentialEmpID
        FOREIGN KEY (EmpID)
            REFERENCES Employee(EmpID)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);

DROP TABLE IF EXISTS Timesheet;
CREATE TABLE Timesheet(
	TimesheetID VARCHAR(255) NOT NULL UNIQUE,
    EmpID INT NOT NULL,
	EndWeek DATE NOT NULL,
    Overtime INT,
    Flextime INT,
	Status ENUM('pending', 'submitted', 'approved', 'denied') NOT NULL DEFAULT 'pending',
    ReviewedBy INT,
    Signature TINYTEXT,
    Feedback TINYTEXT,
	CreatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
	UpdatedAt TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ApprovedAt TIMESTAMP,
	CONSTRAINT PKTimesheetID 
		PRIMARY KEY (TimesheetID),
	CONSTRAINT FKTimesheetRevieweer 
		FOREIGN KEY (ReviewedBy) 
			REFERENCES Employee(EmpID),
    CONSTRAINT FKTimesheetCreator 
    	FOREIGN KEY (EmpID) 
    		REFERENCES Employee(EmpID)
			ON UPDATE CASCADE
        	ON DELETE CASCADE
);

DROP TABLE IF EXISTS TimesheetRow;
CREATE TABLE TimesheetRow(
	TimesheetID VARCHAR(255) NOT NULL,
	ProjectID VARCHAR(20),
	WorkPackageID VARCHAR(20),
	Notes TINYTEXT,
	Hours BIGINT,
	CONSTRAINT PKTimesheetRowID 
		PRIMARY KEY(ProjectID, WorkPackageID, TimesheetID),
	CONSTRAINT FKTimesheetRow_Timesheet 
		FOREIGN KEY (TimesheetID) REFERENCES Timesheet(TimesheetID)
			ON UPDATE CASCADE
        	ON DELETE CASCADE
);

DROP TABLE IF EXISTS LeaveRequest;
CREATE TABLE LeaveRequest(
	LeaveRequestID VARCHAR(255) NOT NULL UNIQUE,
    EmpID VARCHAR(255) NOT NULL,
    StartDate DATE,
    EndDate DATE,
    Type VARCHAR(125),
    Description VARCHAR(255),
	CONSTRAINT PKLeaveRequestLeaveRequestID PRIMARY KEY (LeaveRequestID),
    CONSTRAINT FKRequestLeaveEmpID
        FOREIGN KEY (EmpID)
            REFERENCES Employee(EmpID)
            ON UPDATE CASCADE
            ON DELETE CASCADE
);



INSERT INTO PayGrade (LabourGrade, ChargeRate) VALUES ("PS", 3.50);

INSERT INTO Employee (EmpID, EmpName, LabourGrade) VALUES (1, "Bruce Link",  "PS");
INSERT INTO Employee (EmpID, EmpName, LabourGrade) VALUES (2, "Yogesh Verma",  "PS");

INSERT INTO Credential (EmpID, EmpUserName, EmpPassword) VALUES (1, "bdlink", "bruce");
INSERT INTO Credential (EmpID, EmpUserName, EmpPassword) VALUES (2, "yogiduzit", "yogesh");

INSERT INTO Timesheet (TimesheetID, EmpID, EndWeek) VALUES ("55000000-0000-0000-0000-000000000000", 1, DATE '2000/3/10');
INSERT INTO Timesheet (TimesheetID, EmpID, EndWeek) VALUES ("26000000-0000-0000-0000-000000000000", 1, DATE '2000/3/17');
INSERT INTO Timesheet (TimesheetID, EmpID, EndWeek) VALUES ("45700000-0000-0000-0000-000000000000", 2, DATE '2000/3/24');
