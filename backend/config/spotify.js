const SpotifyWebApi = require('spotify-web-api-node');

// Create a new instance of the Spotify API
const spotifyApi = new SpotifyWebApi({
  clientId: process.env.SPOTIFY_CLIENT_ID,
  clientSecret: process.env.SPOTIFY_CLIENT_SECRET
});

// Function to get access token
const getAccessToken = async () => {
  try {
    const data = await spotifyApi.clientCredentialsGrant();
    spotifyApi.setAccessToken(data.body['access_token']);
    console.log('Spotify access token retrieved successfully');
    return true;
  } catch (error) {
    console.error('Error getting Spotify access token:', error.message);
    if (error.body) console.error('Spotify error details:', error.body);
    return false;
  }
};

// Initialize the Spotify API
const init = async () => {
  try {
    await getAccessToken();
    // Set up token refresh before it expires (55 minutes)
    setInterval(getAccessToken, 55 * 60 * 1000);
  } catch (error) {
    console.error('Failed to initialize Spotify API:', error);
  }
};

// Initialize immediately
init().catch(console.error);

// Export the spotifyApi instance and getAccessToken function
module.exports = {
  spotifyApi,
  getAccessToken
};