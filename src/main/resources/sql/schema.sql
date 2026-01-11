-- docker run -e "ACCEPT_EULA=Y" -e "SA_PASSWORD=R00t@sql" -p 1433:1433 --name dockerized-sql -d mcr.microsoft.com/mssql/server
USE master;

IF NOT EXISTS (SELECT *
               FROM SYS.databases
               WHERE name = N'DIGIPAY')
CREATE DATABASE DIGIPAY;

USE DIGIPAY;

IF NOT EXISTS (SELECT 1
               FROM SYS.TABLES
               WHERE name = 'REGISTRATION')
CREATE TABLE [DIGIPAY].[DBO].[REGISTRATION]
(
    [ID]             UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    [REQUEST_ID]     VARCHAR(100) NOT NULL,
    [NAMES]          NVARCHAR(50) NOT NULL,
    [ACCOUNT]        NVARCHAR(20) NOT NULL,
    [MAPPED_ACCOUNT] NVARCHAR(20) NOT NULL,
    [PROXY]          NVARCHAR(25),
    [MSISDN]         NVARCHAR(20) NOT NULL,
    [EMAIL]          NVARCHAR(50),
    [IDENTIFIER]     NVARCHAR(50) NOT NULL,
    [DESCRIPTION]    NVARCHAR(100),
    [STATUS]         NVARCHAR(15) NOT NULL,
    [TIMESTAMP]      DATETIME                     DEFAULT GETDATE()
);

IF NOT EXISTS (SELECT 1
               FROM SYS.TABLES
               WHERE NAME = 'BANKS')
CREATE TABLE [DIGIPAY].[DBO].[BANKS]
(
    [ID]           UNIQUEIDENTIFIER PRIMARY KEY DEFAULT NEWID(),
    [BANKNAME]     NVARCHAR(50) NOT NULL,
    [ABBREVIATION] NVARCHAR(100),
    [SORTCODE]     NVARCHAR(15) NOT NULL,
    [SWIFTCODE]    NVARCHAR(15) NOT NULL,
    [STATUS]       BIT          NOT NULL        DEFAULT 1,
    [TIMESTAMP]    DATETIME                     DEFAULT GETDATE()
);

IF NOT EXISTS (SELECT 1
               FROM SYS.TABLES
               WHERE NAME = 'TRANSACTIONS')
CREATE TABLE [DIGIPAY].[DBO].[TRANSACTIONS]
(
    ID             UNIQUEIDENTIFIER        DEFAULT NEWID() PRIMARY KEY,
    REQUEST_ID     VARCHAR(50)    NOT NULL,
    DOCUMENT       VARCHAR(50),
    SENDER_ID      VARCHAR(50)    NOT NULL,
    RECEIVER_ID    VARCHAR(50)    NOT NULL,
    RECEIVER_NAMES VARCHAR(50),
    BANK_CODE      VARCHAR(10),
    BANK_NAME      VARCHAR(25),
    AMOUNT         DECIMAL(18, 2) NOT NULL,
    STATUS         VARCHAR(10)    NOT NULL,
    ACKNOWLEDGED   BIT,
    INWARD         BIT            NOT NULL DEFAULT 0,
    MESSAGE        VARCHAR(500),
    DATE           DATETIME       NOT NULL DEFAULT GETDATE()
);

IF NOT EXISTS (SELECT 1
               FROM SYS.TABLES
               WHERE NAME = 'MODIFICATIONS')
CREATE TABLE DBO.MODIFICATIONS
(
    ID              int identity (1000000, 1)
        primary key,
    REQUEST_ID      varchar(50) not null,
    IS_ADD_PROXY    bit         default 0,
    IS_MOD_PROXY    bit         default 0,
    IS_DEL_PROXY    bit         default 0,
    IS_ADD_MAIL     bit         default 0,
    REGISTRATION_ID uniqueidentifier,
    ACCOUNT         varchar(20),
    TIMESTAMP       datetime    default getdate(),
    STATUS          varchar(50) default 'PENDING',
    DESCRIPTION     varchar(50)
);

IF NOT EXISTS (SELECT 1
               FROM SYS.TABLES
               WHERE NAME = 'TEMP_MODIFICATIONS')
CREATE TABLE TEMP_MODIFICATIONS
(
    ID              INT IDENTITY (1000000, 1)
        PRIMARY KEY,
    MODIFICATION_ID INT         NOT NULL REFERENCES MODIFICATIONS,
    REQUEST_ID      varchar(50) not null,
    MOD_TYPE        varchar(15) not null,
    OLD_VALUE       varchar(50) not null,
    NEW_VALUE       varchar(50) not null,
    REGISTRATION_ID uniqueidentifier,
    TIMESTAMP       datetime    default getdate(),
    STATUS          varchar(50) default 'PENDING'
);

IF OBJECT_ID('DBO.SP_GETBANKBYCODE', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [SP_GETBANKBYCODE] @BANK_CODE VARCHAR(10)
                AS
                BEGIN
            SELECT ABBREVIATION Name, BANKNAME ''Full Name''
            from [DBO].[BANKS] WHERE SORTCODE = @BANK_CODE or SWIFTCODE = @BANK_CODE
            END')
    END;

IF OBJECT_ID('DBO.SP_GET_ACCOUNT_BY', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [DBO].[SP_GET_ACCOUNT_BY] @IDENTIFIER VARCHAR(50)
                AS
                BEGIN
                    SELECT ISNULL(ACCOUNT, ''UNKNWN'') ACCOUNT, ISNULL(MAPPED_ACCOUNT, ''UNKNWN'') MAPPED_ACCOUNT
                    FROM [DBO].[REGISTRATION]
                    WHERE ACCOUNT = @IDENTIFIER AND STATUS = ''ACCEPTED'';
                END')
    END;

IF OBJECT_ID('DBO.SP_GET_ACCOUNT_TRANSACTIONS_PROPERTIES', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [DBO].[SP_GET_ACCOUNT_TRANSACTIONS_PROPERTIES] @SENDER_ID VARCHAR(50)
                AS
                    BEGIN TRY
                        SELECT ISNULL(SUM(AMOUNT), 0.0)          AS VALUE,
                               ISNULL(COUNT(AMOUNT), 0)          AS VOLUME,
                               IIF(SUM(AMOUNT) > 10000000, 1, 0) AS EXCEEDED,
                               CAST(GETDATE() AS DATE)           AS DATE
                        FROM [DIGIPAY].[DBO].[TRANSACTIONS] T
                                 LEFT JOIN [DIGIPAY].[DBO].[REGISTRATION] R ON T.SENDER_ID = R.ACCOUNT
                        WHERE T.SENDER_ID = @SENDER_ID
                          AND CAST(DATE AS DATE) = CAST(GETDATE() AS DATE)
                        GROUP BY CAST(DATE AS DATE);
                    END TRY
                    BEGIN CATCH
                        SELECT ERROR_MESSAGE() AS MESSAGE;
                    END CATCH')
    END;

IF OBJECT_ID('DBO.SP_UPDATE_MODIFICATION', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [DBO].[SP_UPDATE_MODIFICATION] @REQUEST_ID VARCHAR(50),
                                                @STATUS VARCHAR(50)
                AS
                BEGIN
                    DECLARE @MODIFICATION_ID VARCHAR(50),
                        @MODIFICATION_TYPE VARCHAR(50),
                        @NEW_VALUE VARCHAR(50),
                        /****  FOR NOTIFICATION  ****/
                        @NAMES VARCHAR(50),
                        @RECIPIENT VARCHAR(15),
                        @SOURCE VARCHAR(15),
                        @OPERATION VARCHAR(20),
                        @MESSAGE VARCHAR(MAX),
                        /****  FOR NOTIFICATION  ****/
                        @REGISTRATION_ID UNIQUEIDENTIFIER;

                    SELECT @MODIFICATION_ID = [MODIFICATION_ID],
                           @MODIFICATION_TYPE = [MOD_TYPE],
                           @NEW_VALUE = [NEW_VALUE],
                           @REGISTRATION_ID = [REGISTRATION_ID]
                    FROM TEMP_MODIFICATIONS
                    WHERE [REQUEST_ID] = @REQUEST_ID;

                    SELECT @NAMES = ISNULL(SUBSTRING([NAMES], 1, CHARINDEX('' '', [NAMES]) - 1), ''customer''),
                           @RECIPIENT = MSISDN
                    FROM [DBO].[REGISTRATION]
                    WHERE [ID] = @REGISTRATION_ID;

                    IF @MODIFICATION_TYPE = ''ADD_MAIL''
                        BEGIN
                            UPDATE [REGISTRATION] SET [EMAIL] = @NEW_VALUE WHERE [ID] = @REGISTRATION_ID;
                            UPDATE [MODIFICATIONS] SET [STATUS] = @STATUS WHERE [ID] = @MODIFICATION_ID AND [REQUEST_ID] = @REQUEST_ID;
                        END;

                    IF @MODIFICATION_TYPE = ''ADD_PROXY''
                        BEGIN
                            UPDATE [REGISTRATION] SET [PROXY] = @NEW_VALUE WHERE [ID] = @REGISTRATION_ID;
                            UPDATE [MODIFICATIONS] SET [STATUS] = @STATUS WHERE [ID] = @MODIFICATION_ID AND [REQUEST_ID] = @REQUEST_ID;
                        END;

                    IF @MODIFICATION_TYPE = ''MOD_PROXY''
                        BEGIN
                            UPDATE REGISTRATION SET [PROXY] = @NEW_VALUE WHERE [ID] = @REGISTRATION_ID;
                            UPDATE MODIFICATIONS SET [STATUS] = @STATUS WHERE [ID] = @MODIFICATION_ID AND [REQUEST_ID] = @REQUEST_ID;
                        END;

                    IF @MODIFICATION_TYPE = ''DEL_PROXY''
                        BEGIN
                            UPDATE REGISTRATION SET [PROXY] = @NEW_VALUE WHERE [ID] = @REGISTRATION_ID;
                            UPDATE MODIFICATIONS SET [STATUS] = @STATUS WHERE [ID] = @MODIFICATION_ID AND [REQUEST_ID] = @REQUEST_ID;
                        END;

                    DELETE FROM TEMP_MODIFICATIONS WHERE [REQUEST_ID] = @REQUEST_ID;

                    /****  FOR NOTIFICATION  ****/
                    /*IF @STATUS = ''APPROVED''
                        BEGIN
                            EXEC [DBO].[SP_SEND_NOTIFICATION] @NAMES, @MSISDN, ''MODIFICATION APPROVED'';
                        END;
                    ELSE
                        BEGIN
                            EXEC [DBO].[SP_SEND_NOTIFICATION] @NAMES, @MSISDN, ''MODIFICATION REJECTED'';
                        END;*/
                    SET @OPERATION = CASE
                                         WHEN @MODIFICATION_TYPE = ''ADD_MAIL'' THEN '' to add email address''
                                         WHEN @MODIFICATION_TYPE = ''ADD_PROXY'' THEN '' to add proxy''
                                         WHEN @MODIFICATION_TYPE = ''MOD_PROXY'' THEN '' to modify proxy''
                                         WHEN @MODIFICATION_TYPE = ''DEL_PROXY'' THEN '' to delete proxy''
                                         ELSE ''UNKNOWN''
                        END;

                    SET @STATUS = CASE
                                      WHEN (@STATUS = ''ACSC'' OR @STATUS = ''ACCP'') THEN ''completed''
                                      WHEN @STATUS = ''CANC'' THEN ''cancelled''
                                      WHEN @STATUS = ''RJCT'' THEN ''rejected''
                                      ELSE ''acknowledged'' END;

                    SET @MESSAGE =
                            CONCAT(''Dear '', @NAMES, '', the modification'', @OPERATION, '' with reference '', @REQUEST_ID, '' is now '',
                                   @STATUS, ''.'',
                                   CHAR(13), CHAR(10),
                                   ''Date: '', FORMAT(GETDATE(), ''dd-MMM-yyyy HH:mm''),
                                   CHAR(13), CHAR(10),
                                   ''Wouldn''''t you rather bank with us?'',
                                   CHAR(13), CHAR(10),
                                   ''www.gtbank.co.rw'');

                    SET @SOURCE = ''GT-eKash'';
                END ;

                    PRINT @MESSAGE;
                    PRINT @RECIPIENT;
                    PRINT @NAMES;
                    PRINT @SOURCE;
                BEGIN TRY
                    EXEC [SMSALERT].[DBO].[SEND_SMS]
                         @SOURCE,
                         @RECIPIENT,
                         @MESSAGE, 0, NULL
                END TRY
                BEGIN CATCH
                    THROW;
                END CATCH;')
    END;

IF OBJECT_ID('DBO.SP_CREATE_NEW_ACCOUNT', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE
                    [DBO].[SP_CREATE_NEW_ACCOUNT] @REQUEST_ID VARCHAR(50),
                                                  @ACCOUNT VARCHAR(50),
                                                  @MAPPED VARCHAR(25),
                                                  @PROXY VARCHAR(11) = NULL,
                                                  @NAMES VARCHAR(50),
                                                  @MSISDN VARCHAR(15),
                                                  @EMAIL VARCHAR(50),
                                                  @IDENTIFIER VARCHAR(50),
                                                  @STATUS VARCHAR(10),
                                                  @DESCRIPTION VARCHAR(100) = NULL
                AS
                BEGIN
                    BEGIN TRY
                        INSERT INTO [DBO].[REGISTRATION]
                            (REQUEST_ID, NAMES, ACCOUNT, MAPPED_ACCOUNT, PROXY, MSISDN, EMAIL, IDENTIFIER,
                                                          DESCRIPTION, STATUS, TIMESTAMP)
                        VALUES
                            (@REQUEST_ID, @NAMES, @ACCOUNT, @MAPPED, @PROXY, @MSISDN, @EMAIL, @IDENTIFIER, @DESCRIPTION, @STATUS,
                                GETDATE());
                    END TRY
                    BEGIN CATCH
                        INSERT INTO [VASDB].[VAS].[SERVICE_TX_ERRORS]
                            (SERVICE_CODE, PARTNER_ID, REFERENCE, ERROR, ERROR_DESCRIPTION)
                        VALUES
                            (''DIGIPAY'', ''REGISTER'', @REQUEST_ID, ERROR_NUMBER(), ERROR_MESSAGE());
                        THROW;
                    END CATCH
                END')
    END;

IF OBJECT_ID('DBO.SP_GET_TRANSACTION_BY', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [DBO].[SP_GET_TRANSACTION_BY] @E2E_ID VARCHAR(50)
                AS
            BEGIN
                SELECT T.DOCUMENT AS DOC_NUM, R.MAPPED_ACCOUNT AS ACCOUNT, FORMAT(T.AMOUNT, ''N'') AS AMOUNT
                FROM TRANSACTIONS T
                         INNER JOIN REGISTRATION R ON T.RECEIVER_ID = R.ACCOUNT
                WHERE INWARD = 1
                  AND T.REQUEST_ID = @E2E_ID
                GROUP BY T.DOCUMENT, R.MAPPED_ACCOUNT, AMOUNT
            END')
    END;

IF OBJECT_ID('DBO.SP_GET_SAVED_REGISTRATION_BY', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [DBO].[SP_GET_SAVED_REGISTRATION_BY] @REQUEST_ID VARCHAR(50)
                AS
                BEGIN
                    SELECT R.[ACCOUNT],
                           R.[MSISDN],
                           R.[STATUS],
                           CAST(IIF(M.ID IS NULL, 0, 1) AS BIT) AS IS_ACMT,
                           ISNULL(CASE
                                      WHEN [IS_ADD_PROXY] = 1 THEN ''ADD_PROXY''
                                      WHEN [IS_MOD_PROXY] = 1 THEN ''MOD_PROXY''
                                      WHEN [IS_DEL_PROXY] = 1 THEN ''DEL_PROXY''
                                      WHEN [IS_ADD_MAIL] = 1 THEN ''ADD_MAIL''
                                      END, ''NOT_SET'')           AS MOD_TYPE
                    FROM [DBO].[REGISTRATION] R
                             LEFT JOIN [DBO].[MODIFICATIONS] M ON R.ID = M.REGISTRATION_ID
                    WHERE M.[REQUEST_ID] = @REQUEST_ID;
            END')
    END;

IF OBJECT_ID('DBO.SP_TRANSACT', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE [dbo].[SP_TRANSACT] @REQUEST_ID VARCHAR(50),
                                     @DOCUMENT VARCHAR(50) = NULL,
                                     @STATUS VARCHAR(10) = NULL,
                                     @SENDER_ID VARCHAR(50) = NULL,
                                     @RECEIVER_ID VARCHAR(50) = NULL,
                                     @RECEIVER_NAMES VARCHAR(50) = NULL,
                                     @BANK_CODE VARCHAR(10) = NULL,
                                     @AMOUNT DECIMAL(18, 2),
                                     @ACKNOWLEDGED BIT = NULL,
                                     @INWARD BIT = NULL,
                                     @MESSAGE VARCHAR(500) = NULL,
                                     @INITIATOR VARCHAR(100) = NULL
AS
BEGIN
    DECLARE @BANK_NAME VARCHAR(25);
    DECLARE @SMS VARCHAR(255);
    DECLARE @RECIPIENT VARCHAR(25);
    DECLARE @SENDER_NAME VARCHAR(50);
    DECLARE @SOURCE VARCHAR(15);

    IF (@STATUS IS NULL)
        BEGIN
            SET @STATUS = ''SENT''
        END;

    BEGIN TRY
        SET @BANK_NAME = (SELECT TOP 1 [BANKNAME] FROM [DBO].[BANKS] WHERE [SORTCODE] = @BANK_CODE)

        IF (@DOCUMENT IS NULL)
            BEGIN
                INSERT INTO [DBO].[TRANSACTIONS] (STATUS, REQUEST_ID, DOCUMENT, SENDER_ID, RECEIVER_ID, RECEIVER_NAMES,
                                                  BANK_CODE,
                                                  BANK_NAME, AMOUNT, ACKNOWLEDGED, INWARD, MESSAGE, DATE)
                VALUES (@STATUS, @REQUEST_ID, @DOCUMENT, @SENDER_ID, @RECEIVER_ID, @RECEIVER_NAMES, @BANK_CODE,
                        @BANK_NAME,
                        @AMOUNT,
                        @ACKNOWLEDGED, @INWARD, @MESSAGE, GETDATE());
            END;
        ELSE
            BEGIN
                SELECT @SENDER_ID = SENDER_ID, @AMOUNT = AMOUNT
                FROM [DIGIPAY].[DBO].[TRANSACTIONS]
                WHERE REQUEST_ID = @REQUEST_ID;

                SELECT @RECIPIENT = MSISDN, @SENDER_NAME = SUBSTRING(NAMES, 1, CHARINDEX('' '', NAMES))
                FROM [DIGIPAY].[DBO].[REGISTRATION]
                WHERE ACCOUNT = IIF(@INWARD = 1, @RECEIVER_ID, @SENDER_ID);
                PRINT @RECIPIENT

                SET @SOURCE = ''GT-eKash'';

                IF (@INWARD = 1)
                    BEGIN
                        SET @SMS = ''Rwf'' + FORMAT(@AMOUNT, ''N'') + '' credit on account '' + @RECEIVER_ID + '' from '' +
                                   @INITIATOR + ''.'' + CHAR(13) + CHAR(10) +
                                   ''Reference: '' + @DOCUMENT + ''.'' + CHAR(13) + CHAR(10) +
                                   ''Register for GeNS to get balances updates.'' +
                                   CHAR(13) + CHAR(10) +
                                   ''Wouldn''''t you rather bank with us?'' +
                                   CHAR(13) + CHAR(10) + ''www.gtbank.co.rw'';
                    END;
                ELSE
                    BEGIN
                        SET @STATUS = CASE
                                  WHEN @STATUS = ''ACSC'' THEN ''completed''
                                  WHEN @STATUS = ''CANC'' THEN ''cancelled''
                                  WHEN @STATUS = ''RJCT'' THEN ''rejected''
                                  ELSE ''acknowledged'' END;

                        SET @SMS = ''The operation '' + @REQUEST_ID + '' for Rwf'' + FORMAT(@AMOUNT, ''N'') +
                                   '' from your account '' + @SENDER_ID + '' was '' + @STATUS + ''.'' + CHAR(13) + CHAR(10) +
                                   ''Wouldn''''t you rather bank with us?'' + CHAR(13) + CHAR(10) + ''www.gtbank.co.rw'';
                    END;

                PRINT @SMS

                UPDATE [DBO].[TRANSACTIONS]
                SET STATUS   = @STATUS,
                    DOCUMENT = @DOCUMENT,
                    MESSAGE  = @MESSAGE,
                    DATE     = GETDATE()
                WHERE REQUEST_ID = @REQUEST_ID;

                PRINT @SOURCE
                PRINT @RECIPIENT
                PRINT @SMS

                EXEC [SMSALERT].[DBO].[SEND_SMS] @SOURCE, @RECIPIENT, @SMS, 0, NULL;
            END;

    END TRY
    BEGIN CATCH
        INSERT INTO [VASDB].[VAS].[SERVICE_TX_ERRORS] (SERVICE_CODE, PARTNER_ID, REFERENCE, ERROR, ERROR_DESCRIPTION, DATE)
        VALUES (''DIGIPAY'', @BANK_CODE, @REQUEST_ID, ERROR_NUMBER(),
                ERROR_MESSAGE(), GETDATE());
        THROW;
    END CATCH
END;')
    END;

IF OBJECT_ID('DBO.SP_ACMT_UPDATE_REGISTRATION', 'P') IS NULL
    BEGIN
        EXEC ('CREATE PROCEDURE
                [DBO].[SP_ACMT_UPDATE_REGISTRATION]
                @REQUEST_ID VARCHAR(50),
                @STATUS VARCHAR(15),
                @REASON VARCHAR(75) = NULL
                AS
                BEGIN
                    DECLARE @EMAIL VARCHAR(50);
                    DECLARE @RECIPIENT VARCHAR(50);
                    DECLARE @NAME VARCHAR(20);
                    DECLARE @ACCOUNT VARCHAR(20);
                    DECLARE @MESSAGE VARCHAR(255);
                    DECLARE @SOURCE VARCHAR(10);

                    BEGIN
                        SELECT @ACCOUNT = ACCOUNT,
                               @RECIPIENT = MSISDN,
                               @EMAIL = ISNULL(EMAIL, ''info@gtbank.com''),
                               @NAME = SUBSTRING(NAMES, 1, CHARINDEX('' '', NAMES))
                        FROM [DBO].[REGISTRATION]
                        WHERE REQUEST_ID = @REQUEST_ID;

                        SET @MESSAGE =
                                CONCAT(''Dear '', @NAME, '', the operation with reference '', @REQUEST_ID, '' is now '', @STATUS, ''.'',
                                       CHAR(13), CHAR(10),
                                       ''The details are:'', CHAR(13), CHAR(10), ''Account: '', @ACCOUNT,
                                       CHAR(13), CHAR(10),
                                       -- SHOULD INCLUDE ONLY IF THE REASON IS NOT NULL
                                       IIF(@REASON IS NOT NULL, CONCAT(''Reason: '', @REASON, CHAR(13), CHAR(10)), ''''),
                                       ''Date: '', FORMAT(GETDATE(), ''dd-MMM-yyyy HH:mm''),
                                       CHAR(13), CHAR(10),
                                       ''Wouldn''''t you rather bank with us?'',
                                       CHAR(13), CHAR(10),
                                       ''www.gtbank.co.rw'');

                        SET @SOURCE = ''GT-eKash'';
                    END ;
                        PRINT @MESSAGE;
                PRINT @RECIPIENT;
                PRINT @NAME;
                PRINT @SOURCE;
                    UPDATE [DIGIPAY].[DBO].[REGISTRATION]
                    SET [STATUS]      = UPPER(@STATUS),
                        [DESCRIPTION] = CONCAT(@REQUEST_ID, '' CALLBACK AT '', FORMAT(GETDATE(), ''dd-MMM-yyyy HH:mm''))
                    WHERE [REQUEST_ID] = @REQUEST_ID;

                    BEGIN TRY
                        EXEC [SMSALERT].[DBO].[SEND_SMS]
                             ''GT-eKash'',
                             @RECIPIENT,
                             @MESSAGE, 0, NULL
                    END TRY
                    BEGIN CATCH
                        THROW;
                    END CATCH;
                END')
    END;

IF OBJECT_ID('DBO.SP_CREATE_MODIFICATION', 'P') IS NULL
    BEGIN
        EXEC ('CREATE   PROCEDURE SP_CREATE_MODIFICATION @REQUEST_ID VARCHAR(50),
                                        @ACCOUNT VARCHAR(50),
                                        @IS_ADD_PROXY BIT,
                                        @IS_MOD_PROXY BIT,
                                        @IS_DEL_PROXY BIT,
                                        @IS_ADD_MAIL BIT,
                                        @NEW_VALUE VARCHAR(50) = NULL
                AS
                BEGIN
                    DECLARE @REGISTRATION_ID VARCHAR(100);
                    DECLARE @STATUS VARCHAR(50);
                    DECLARE @DESCRIPTION VARCHAR(50);
                    DECLARE @OLD_VALUE VARCHAR(50);
                    DECLARE @PROCEED BIT = 1;
                    DECLARE @MODIFICATION_ID INT;

                    SET @REGISTRATION_ID = (SELECT [ID] FROM [DIGIPAY].[DBO].[REGISTRATION] WHERE ACCOUNT = @ACCOUNT);
                    SET @STATUS = (SELECT [STATUS] FROM [DIGIPAY].[DBO].[REGISTRATION] WHERE ACCOUNT = @ACCOUNT);
                    SET @DESCRIPTION = CASE
                                           WHEN @IS_ADD_PROXY = 1 THEN CONCAT(@ACCOUNT, '' ADD PROXY'')
                                           WHEN @IS_MOD_PROXY = 1 THEN CONCAT(@ACCOUNT, '' MODIFY PROXY'')
                                           WHEN @IS_DEL_PROXY = 1 THEN CONCAT(@ACCOUNT, '' DELETE PROXY'')
                                           WHEN @IS_ADD_MAIL = 1 THEN CONCAT(@ACCOUNT, '' ADD MAIL'')
                                           ELSE ''UNKNOWN''
                        END;

                    IF @REGISTRATION_ID IS NULL
                        BEGIN
                            SET @PROCEED = 0;
                            RAISERROR (''REGISTRATION NOT FOUND'', 16, 1);
                        END;

                    IF @STATUS = ''PENDING''
                        BEGIN
                            SET @PROCEED = 0;
                            RAISERROR (''REGISTRATION NOT APPROVED'', 16, 1);
                        END;

                    IF @IS_ADD_MAIL = 1
                        BEGIN
                            IF @NEW_VALUE IS NULL
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''MAIL ID CANNOT BE NULL'', 16, 1);
                                END;
                            BEGIN
                                SET @OLD_VALUE = (SELECT ISNULL([EMAIL], ''NOT SET'')
                                                  FROM [DIGIPAY].[DBO].[REGISTRATION]
                                                  WHERE ACCOUNT = @ACCOUNT);
                            END;
                        END;

                    IF @IS_ADD_PROXY = 1
                        BEGIN
                            IF @NEW_VALUE IS NULL
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''PROXY ID CANNOT BE NULL'', 16, 1);
                                END;

                            IF EXISTS (SELECT * FROM [DIGIPAY].[DBO].[REGISTRATION] WHERE PROXY = @NEW_VALUE)
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''PROXY ID ALREADY EXISTS'', 16, 1);
                                END;
                            BEGIN
                                SET @OLD_VALUE = (SELECT ISNULL([PROXY], ''NOT SET'')
                                                  FROM [DIGIPAY].[DBO].[REGISTRATION]
                                                  WHERE ACCOUNT = @ACCOUNT);
                            END;
                        END;

                    IF @IS_MOD_PROXY = 1
                        BEGIN
                            IF @NEW_VALUE IS NULL
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''PROXY ID CANNOT BE NULL'', 16, 1);
                                END;
                            BEGIN
                                SET @OLD_VALUE = (SELECT ISNULL([PROXY], ''NOT SET'')
                                                  FROM [DIGIPAY].[DBO].[REGISTRATION]
                                                  WHERE ACCOUNT = @ACCOUNT);
                            END;
                        END;

                    IF @IS_DEL_PROXY = 1
                        BEGIN
                            IF @NEW_VALUE IS NULL
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''PROXY ID CANNOT BE NULL'', 16, 1);
                                END;

                            IF NOT EXISTS (SELECT * FROM [DIGIPAY].[DBO].[REGISTRATION] WHERE PROXY = @NEW_VALUE)
                                BEGIN
                                    SET @PROCEED = 0;
                                    RAISERROR (''PROXY ID DOES NOT EXIST'', 16, 1);
                                END;
                            BEGIN
                                SET @OLD_VALUE = (SELECT ISNULL([PROXY], ''NOT SET'')
                                FROM [DIGIPAY].[DBO].[REGISTRATION]
                                WHERE ACCOUNT = @ACCOUNT);
                            END;
                        END;
                    IF @PROCEED = 1
                        BEGIN
                        BEGIN
                            INSERT INTO [DIGIPAY].[DBO].[MODIFICATIONS] ([REQUEST_ID], [REGISTRATION_ID], [ACCOUNT], [IS_ADD_PROXY],
                                                                         [IS_MOD_PROXY],
                                                                         [IS_DEL_PROXY], [IS_ADD_MAIL], [TIMESTAMP], [STATUS],
                                                                         [DESCRIPTION])
                            VALUES (@REQUEST_ID, @REGISTRATION_ID, @ACCOUNT, @IS_ADD_PROXY, @IS_MOD_PROXY, @IS_DEL_PROXY, @IS_ADD_MAIL,
                                    GETDATE(),
                                    ''PENDING'',
                                    @DESCRIPTION);
                            SET @MODIFICATION_ID = @@IDENTITY;
                        END;
                        BEGIN
                            INSERT INTO [DBO].[TEMP_MODIFICATIONS] (MODIFICATION_ID, REQUEST_ID, MOD_TYPE, OLD_VALUE, NEW_VALUE, REGISTRATION_ID)
                            VALUES (@MODIFICATION_ID, @REQUEST_ID,
                                    CASE
                                        WHEN @IS_ADD_PROXY = 1 THEN ''ADD_PROXY''
                                        WHEN @IS_MOD_PROXY = 1 THEN ''MOD_PROXY''
                                        WHEN @IS_DEL_PROXY = 1 THEN ''DEL_PROXY''
                                        WHEN @IS_ADD_MAIL = 1 THEN ''ADD_MAIL''
                                        ELSE ''UNKNOWN''
                                    END, @OLD_VALUE, @NEW_VALUE, @REGISTRATION_ID);
                        END;
                        END;
                END;')
    END;

DELETE
FROM [DIGIPAY].[DBO].[BANKS];
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'INVESTMENT AND MORTGAGE BK (BCR)', N'I&M', N'010', N'IMRWRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Letshego Rwanda Plc', N'Letshego', N'021', N'LERSRWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Atlantique Microfinance Plc', N'ATLANTIQUE', N'022', N'AMIFRWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Umutanguha Finance Company Plc', N'UMUTANGUHA', N'023', N'UMUFRWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Duterimbere-IMF Plc', N'DUTERIMBER', N'024', N'DUTERWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'COMMERCIAL BANK OF AFRICA', N'CBA', N'035', N'CBAFRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Goshen Finance Plc', N'GOSHEN', N'026', N'GOSHRWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'COPEDU Plc', N'COPEDU', N'602', N'COPERWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Financial Safety Co Plc', N'FINANCIAL', N'029', N'FISARWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'Inkunga Finance Plc', N'INKUNGA', N'031', N'INKURWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'RIM LIMITED', N'RIM LTD', N'032', N'RIMIRWIP');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'BANQUE DE KIGALI', N'BK', N'040', N'BKIGRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'UMWALIMU SACCO', N'UMWALIMU', N'090', N'UMWARWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'ECOBANK', N'ECOBANK', N'011', N'ECOCRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'ACCESS RWANDA', N'ACCESS', N'115', N'BKORRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'COMPAGNIE GENERALE DE BANQ.', N'COGEBANQUE', N'030', N'CGBKRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'URWEGO OPPORTUNITY BANK', N'UOB', N'145', N'UOBRRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'EQUITY BANK', N'EQUITY', N'192', N'EQBLRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'BANQUE POPULAIRE DU RWANDA', N'BPR', N'044', N'BPRWRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'AB Bank', N'AB BANK', N'020', N'ABBRRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'BANQUE RWANDAISE DE DEVE.', N'BRD', N'750', N'BRDRRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'ZIGAMA CREDIT AND SAVINGS', N'ZIGAMA CSS', N'075', N'ZCSSRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'BANK OF AFRICA RWANDA LIMITED', N'BOA', N'900', N'AFRWRWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'UNGUKA BANK', N'UNGUKA', N'950', N'UNGURWRW');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'MTN MOBILE MONEY RWANDA LTD', N'MMRL', N'078', N'MMRL');
INSERT INTO [DIGIPAY].[DBO].[BANKS] (BANKNAME, ABBREVIATION, SORTCODE, SWIFTCODE)
VALUES (N'AIRTEL MONEY RWANDA', N'AIRTEL', N'073', N'AIRTEL');

-- CREATING INDICES ON REGISTRATION TABLE
IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_REGISTRATION_REQUEST_ID')
CREATE INDEX [IDX_REGISTRATION_REQUEST_ID] ON [DIGIPAY].[DBO].[REGISTRATION] ([REQUEST_ID]);

IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_REGISTRATION_ACCOUNT')
CREATE INDEX [IDX_REGISTRATION_ACCOUNT] ON [DIGIPAY].[DBO].[REGISTRATION] ([ACCOUNT]);

IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_REGISTRATION_MAPPED_ACCOUNT')
CREATE INDEX [IDX_REGISTRATION_MAPPED_ACCOUNT] ON [DIGIPAY].[DBO].[REGISTRATION] ([MAPPED_ACCOUNT]);

-- CREATING INDICES ON TRANSACTIONS TABLE
IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_TRANSACTIONS_REQUEST_ID')
CREATE INDEX [IDX_TRANSACTIONS_REQUEST_ID] ON [DIGIPAY].[DBO].[TRANSACTIONS] ([REQUEST_ID]);

IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_TRANSACTIONS_DOCUMENT')
CREATE INDEX [IDX_TRANSACTIONS_DOCUMENT] ON [DIGIPAY].[DBO].[TRANSACTIONS] ([DOCUMENT]);

IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_TRANSACTIONS_SENDER_ID')
CREATE INDEX [IDX_TRANSACTIONS_SENDER_ID] ON [DIGIPAY].[DBO].[TRANSACTIONS] ([SENDER_ID]);

IF NOT EXISTS (SELECT 1
               FROM SYS.INDEXES
               WHERE NAME = 'IDX_TRANSACTIONS_RECEIVER_ID')
CREATE INDEX [IDX_TRANSACTIONS_RECEIVER_ID] ON [DIGIPAY].[DBO].[TRANSACTIONS] ([RECEIVER_ID]);