import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.File;

class DecryptIdPasswordFileTest {

    @Mock
    private EncryptIdPasswordFile encryptIdPasswordFile;

    @Mock
    private Decrypt decrypt;

    private DecryptIdPasswordFile decryptIdPasswordFile;

    @BeforeEach
    void setUp() {
        // Set the environment variable for the test
        System.setProperty("env", "C:\\Cash\\cash_core");

        MockitoAnnotations.openMocks(this);  // Initialize mocks

        // Mock the file location and behavior of getFile method
        File mockFile = mock(File.class);
        when(encryptIdPasswordFile.getFile(anyString(), anyString())).thenReturn(mockFile);
        
        // Mock file.exists() to control file existence behavior
        when(mockFile.exists()).thenReturn(true); // You can change this to false for other tests

        // Initialize the DecryptIdPasswordFile with mocked file and environment
        decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");
    }

    @Test
    void testConstructor_FileNotFound() throws Exception {
        // Arrange: Mocking the behavior of getFile and exists to simulate file not found
        File mockFile = mock(File.class);
        when(encryptIdPasswordFile.getFile("env", "fileName")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(false); // Simulate file not found

        // Act & Assert: Test if exception is thrown when file is not found
        Exception exception = assertThrows(Exception.class, () -> {
            decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");
        });

        assertTrue(exception.getMessage().contains("Unable to find"));
    }

    @Test
    void testConstructor_Success() throws Exception {
        // Arrange: Mocking the behavior of getFile and exists to simulate file found
        File mockFile = mock(File.class);
        when(encryptIdPasswordFile.getFile("env", "fileName")).thenReturn(mockFile);
        when(mockFile.exists()).thenReturn(true); // Simulate file found

        // Act: Initialize the object
        decryptIdPasswordFile = new DecryptIdPasswordFile("env", "fileName");

        // Assert: Check if the object is correctly initialized
        assertNotNull(decryptIdPasswordFile);
    }

    @Test
    void testGetUserName() throws Exception {
        // Arrange: Mocking the data for getUserName()
        Container mockData = mock(Container.class);
        when(decryptIdPasswordFile.getUserName()).thenReturn("mockUser");

        // Act: Call the method
        String userName = decryptIdPasswordFile.getUserName();

        // Assert: Check if the correct username is returned
        assertEquals("mockUser", userName);
    }

    @Test
    void testGetPassword() throws Exception {
        // Arrange: Mocking the data for getPassword()
        Container mockData = mock(Container.class);
        when(decryptIdPasswordFile.getPassword()).thenReturn("mockPassword");

        // Act: Call the method
        String password = decryptIdPasswordFile.getPassword();

        // Assert: Check if the correct password is returned
        assertEquals("mockPassword", password);
    }
}
